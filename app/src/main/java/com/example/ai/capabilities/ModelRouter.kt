package com.example.ai.capabilities

import android.util.Log
import com.example.data.EndpointEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

data class ProviderBenchmarkReport(
    val endpointKey: String,
    val providerType: String,
    val modelName: String,
    val isOnline: Boolean,
    val latencyMs: Long,
    val reliabilityScore: Float,
    val statusMessage: String?
)

data class ProviderStatusEntry(
    val providerName: String,
    val providerType: String,
    val modelName: String,
    val status: ProviderStatus,
    val latencyMs: Long,
    val reliabilityScore: Float,
    val activeCooldownRemainingMs: Long,
    val bestWorkloads: List<WorkloadType>,
    val detailMessage: String
)

data class IntelligenceStatusReport(
    val providerEntries: List<ProviderStatusEntry>,
    val currentMission: String?,
    val selectedWorker: String,
    val fallbackChain: List<String>,
    val activeWorkload: WorkloadType?,
    val evidenceRequirement: String,
    val estimatedCost: String,
    val selectionReason: String
)

data class WorkerSelectionResult(
    val primaryEndpoint: EndpointEntity?,
    val primaryProvider: ModelProvider,
    val fallbackEndpoints: List<EndpointEntity>,
    val selectionReason: String
)

class ModelRouter(
    private val capabilityRegistry: CapabilityRegistry,
    val metricsTracker: ProviderMetricsTracker = ProviderMetricsTracker(),
    val workloadMatrix: WorkloadBenchmarkMatrix = WorkloadBenchmarkMatrix()
) {
    val fallbackProvider = OfflineFallbackProvider()
    val geminiProvider = GeminiProvider()
    val openRouterProvider = OpenRouterProvider()
    val ollamaProvider = OllamaProvider()
    val openAiProvider = OpenAiCompatibleProvider()
    val anthropicProvider = AnthropicDirectProvider()

    private fun log(msg: String) {
        try {
            Log.d("ModelRouter", msg)
        } catch (_: Throwable) {
            println("[ModelRouter] $msg")
        }
    }

    fun getProviderForEndpoint(endpoint: EndpointEntity): ModelProvider {
        val type = endpoint.type.uppercase()
        val providers = capabilityRegistry.getProviders(CapabilityType.MODEL)

        // 1. First check if any registered provider matches providerId or name
        val directMatch = providers.find { 
            (it as? ModelProvider)?.providerId?.uppercase() == type || 
            it.name.uppercase() == type 
        } as? ModelProvider
        if (directMatch != null) return directMatch

        // 2. Standard typed mapping
        return when (type) {
            "GEMINI" -> (providers.find { it is GeminiProvider } as? ModelProvider) ?: geminiProvider
            "ANTHROPIC", "CLAUDE" -> (providers.find { it is AnthropicDirectProvider } as? ModelProvider) ?: anthropicProvider
            "OPENAI", "GROQ", "TOGETHER", "MISTRAL", "DEEPSEEK", "LOCALAI" -> (providers.find { it is OpenAiCompatibleProvider } as? ModelProvider) ?: openAiProvider
            "OLLAMA" -> (providers.find { it is OllamaProvider } as? ModelProvider) ?: ollamaProvider
            "OPENROUTER" -> (providers.find { it is OpenRouterProvider } as? ModelProvider) ?: openRouterProvider
            "OFFLINE" -> fallbackProvider
            else -> {
                when {
                    endpoint.url.contains("googleapis.com") -> geminiProvider
                    endpoint.url.contains("anthropic.com") -> anthropicProvider
                    endpoint.url.contains("11434") || endpoint.url.contains("ollama") -> ollamaProvider
                    endpoint.url.contains("openrouter") -> openRouterProvider
                    else -> (providers.find { it is OpenRouterProvider } as? ModelProvider) ?: openRouterProvider
                }
            }
        }
    }

    /**
     * Selects the optimal worker and fallback chain for a specific WorkloadType
     */
    fun selectWorkerForWorkload(
        workload: WorkloadType,
        endpoints: List<EndpointEntity>,
        requiresVision: Boolean = false,
        requiresTools: Boolean = false
    ): WorkerSelectionResult {
        val prioritized = prioritizeEndpointsForWorkload(endpoints, workload, requiresVision, requiresTools)
        if (prioritized.isEmpty()) {
            return WorkerSelectionResult(
                primaryEndpoint = null,
                primaryProvider = fallbackProvider,
                fallbackEndpoints = emptyList(),
                selectionReason = "No configured remote endpoints available. Engaging Offline Fallback Engine."
            )
        }

        val top = prioritized.first()
        val provider = getProviderForEndpoint(top)
        val score = workloadMatrix.getScore(workload, top.type, top.modelName)
        val reliability = metricsTracker.getReliabilityScore("${top.type}:${top.url}:${top.modelName}")

        val reason = "Selected ${top.type} (${top.modelName}) for $workload (Domain Score: ${"%.2f".format(score)}, Reliability: ${"%.0f".format(reliability * 100)}%)"

        return WorkerSelectionResult(
            primaryEndpoint = top,
            primaryProvider = provider,
            fallbackEndpoints = prioritized.drop(1),
            selectionReason = reason
        )
    }

    /**
     * Generates a model response routed specifically for the given WorkloadType
     */
    suspend fun generateForWorkload(
        workload: WorkloadType,
        endpoints: List<EndpointEntity>,
        systemPrompt: String,
        messages: List<ModelMessage>,
        tools: List<ModelToolDefinition>? = null,
        requiresVision: Boolean = false,
        requiresTools: Boolean = false,
        temperature: Float? = null,
        maxTokens: Int? = null,
        responseFormatJson: Boolean = false
    ): ModelResponse = withContext(Dispatchers.IO) {
        val prioritizedEndpoints = prioritizeEndpointsForWorkload(endpoints, workload, requiresVision, requiresTools)
        val failureLogs = mutableListOf<String>()

        for (endpoint in prioritizedEndpoints) {
            val endpointKey = "${endpoint.type}:${endpoint.url}:${endpoint.modelName}"
            val provider = getProviderForEndpoint(endpoint)
            val startTime = System.currentTimeMillis()

            try {
                val request = ModelRequest(
                    systemPrompt = systemPrompt,
                    messages = messages,
                    endpointConfig = object : EndpointConfig {
                        override val url = endpoint.url
                        override val apiKey = endpoint.apiKey
                        override val modelName = endpoint.modelName
                        override val providerType = endpoint.type
                    },
                    tools = tools,
                    requiresVision = requiresVision,
                    requiresTools = requiresTools,
                    temperature = temperature,
                    maxTokens = maxTokens,
                    responseFormatJson = responseFormatJson
                )

                val response = provider.generate(request)
                val latency = System.currentTimeMillis() - startTime
                metricsTracker.recordSuccess(endpointKey, endpoint.type, latency)
                workloadMatrix.recordEmpiricalResult(workload, endpoint.type, endpoint.modelName, true, latency)
                log("Successfully generated response for $workload via ${provider.name} in ${latency}ms")
                return@withContext response.copy(providerUsed = provider.name, latencyMs = latency)
            } catch (e: Exception) {
                val errorInfo = ProviderErrorClassifier.classify(e)
                metricsTracker.recordFailure(endpointKey, endpoint.type, errorInfo)
                workloadMatrix.recordEmpiricalResult(workload, endpoint.type, endpoint.modelName, false, System.currentTimeMillis() - startTime)
                val logMsg = "[${endpoint.type} - ${endpoint.modelName}]: ${errorInfo.kind} (${errorInfo.rawMessage})"
                failureLogs.add(logMsg)
                log("Provider fallback triggered for $workload. Reason: $logMsg")
            }
        }

        // Engaging Offline Fallback Provider strictly following Reality Contract
        log("All primary providers failed or unconfigured for $workload. Engaging Offline Fallback Provider.")
        val offlineRequest = ModelRequest(
            systemPrompt = systemPrompt,
            messages = messages,
            endpointConfig = object : EndpointConfig {
                override val url = "local://offline"
                override val apiKey = ""
                override val modelName = "offline-deterministic"
                override val providerType = "OFFLINE"
            }
        )
        return@withContext fallbackProvider.generate(offlineRequest)
    }

    suspend fun generate(
        endpoints: List<EndpointEntity>,
        systemPrompt: String,
        messages: List<ModelMessage>,
        tools: List<ModelToolDefinition>? = null,
        requiresVision: Boolean = false,
        requiresTools: Boolean = false,
        temperature: Float? = null,
        maxTokens: Int? = null,
        responseFormatJson: Boolean = false
    ): ModelResponse {
        return generateForWorkload(
            workload = WorkloadType.CODING,
            endpoints = endpoints,
            systemPrompt = systemPrompt,
            messages = messages,
            tools = tools,
            requiresVision = requiresVision,
            requiresTools = requiresTools,
            temperature = temperature,
            maxTokens = maxTokens,
            responseFormatJson = responseFormatJson
        )
    }

    suspend fun streamForWorkload(
        workload: WorkloadType,
        endpoints: List<EndpointEntity>,
        systemPrompt: String,
        messages: List<ModelMessage>,
        tools: List<ModelToolDefinition>? = null,
        requiresVision: Boolean = false,
        requiresTools: Boolean = false
    ): Flow<ModelStream> = flow {
        val prioritizedEndpoints = prioritizeEndpointsForWorkload(endpoints, workload, requiresVision, requiresTools)
        var success = false
        val errors = mutableListOf<String>()

        for (endpoint in prioritizedEndpoints) {
            val endpointKey = "${endpoint.type}:${endpoint.url}:${endpoint.modelName}"
            val provider = getProviderForEndpoint(endpoint)
            val startTime = System.currentTimeMillis()

            try {
                val request = ModelRequest(
                    systemPrompt = systemPrompt,
                    messages = messages,
                    endpointConfig = object : EndpointConfig {
                        override val url = endpoint.url
                        override val apiKey = endpoint.apiKey
                        override val modelName = endpoint.modelName
                        override val providerType = endpoint.type
                    },
                    tools = tools,
                    requiresVision = requiresVision,
                    requiresTools = requiresTools
                )

                var emittedChunks = 0
                provider.stream(request).collect { chunk ->
                    emittedChunks++
                    emit(chunk)
                }

                if (emittedChunks > 0) {
                    val latency = System.currentTimeMillis() - startTime
                    metricsTracker.recordSuccess(endpointKey, endpoint.type, latency)
                    workloadMatrix.recordEmpiricalResult(workload, endpoint.type, endpoint.modelName, true, latency)
                    success = true
                    break
                }
            } catch (e: Exception) {
                val errorInfo = ProviderErrorClassifier.classify(e)
                metricsTracker.recordFailure(endpointKey, endpoint.type, errorInfo)
                workloadMatrix.recordEmpiricalResult(workload, endpoint.type, endpoint.modelName, false, System.currentTimeMillis() - startTime)
                val logMsg = "[${endpoint.type} - ${endpoint.modelName}]: ${errorInfo.kind} (${errorInfo.rawMessage})"
                errors.add(logMsg)
                log("Streaming failed for $workload on $endpointKey, falling back: $logMsg")
            }
        }

        if (!success) {
            log("Streaming fallback to Offline Deterministic Engine for $workload.")
            val offlineRequest = ModelRequest(
                systemPrompt = systemPrompt,
                messages = messages,
                endpointConfig = object : EndpointConfig {
                    override val url = "local://offline"
                    override val apiKey = ""
                    override val modelName = "offline-deterministic"
                    override val providerType = "OFFLINE"
                }
            )
            fallbackProvider.stream(offlineRequest).collect {
                emit(it)
            }
        }
    }

    suspend fun stream(
        endpoints: List<EndpointEntity>,
        systemPrompt: String,
        messages: List<ModelMessage>,
        tools: List<ModelToolDefinition>? = null,
        requiresVision: Boolean = false,
        requiresTools: Boolean = false
    ): Flow<ModelStream> {
        return streamForWorkload(
            workload = WorkloadType.CODING,
            endpoints = endpoints,
            systemPrompt = systemPrompt,
            messages = messages,
            tools = tools,
            requiresVision = requiresVision,
            requiresTools = requiresTools
        )
    }

    /**
     * Prioritizes endpoints taking into account availability, workload benchmarks, and reliability
     */
    fun prioritizeEndpointsForWorkload(
        endpoints: List<EndpointEntity>,
        workload: WorkloadType,
        requiresVision: Boolean,
        requiresTools: Boolean
    ): List<EndpointEntity> {
        if (endpoints.isEmpty()) return emptyList()

        return endpoints.sortedWith { a, b ->
            val keyA = "${a.type}:${a.url}:${a.modelName}"
            val keyB = "${b.type}:${b.url}:${b.modelName}"

            // 1. Availability check (not in active cooldown)
            val availA = metricsTracker.isAvailable(keyA)
            val availB = metricsTracker.isAvailable(keyB)
            if (availA != availB) return@sortedWith if (availA) -1 else 1

            // 2. Capability matching
            val provA = getProviderForEndpoint(a)
            val provB = getProviderForEndpoint(b)
            if (requiresVision) {
                val visA = provA.modelCapabilities.supportsImages
                val visB = provB.modelCapabilities.supportsImages
                if (visA != visB) return@sortedWith if (visA) -1 else 1
            }
            if (requiresTools) {
                val toolA = provA.modelCapabilities.supportsTools
                val toolB = provB.modelCapabilities.supportsTools
                if (toolA != toolB) return@sortedWith if (toolA) -1 else 1
            }

            // 3. Workload Benchmark Score
            val scoreA = workloadMatrix.getScore(workload, a.type, a.modelName)
            val scoreB = workloadMatrix.getScore(workload, b.type, b.modelName)
            if (Math.abs(scoreA - scoreB) > 0.05f) return@sortedWith scoreB.compareTo(scoreA)

            // 4. Primary endpoint preference
            if (a.isPrimary != b.isPrimary) return@sortedWith if (a.isPrimary) -1 else 1

            // 5. Reliability score
            val relA = metricsTracker.getReliabilityScore(keyA)
            val relB = metricsTracker.getReliabilityScore(keyB)
            if (relA != relB) return@sortedWith relB.compareTo(relA)

            // 6. Latency
            val latA = metricsTracker.getMetrics(keyA)?.averageLatencyMs ?: 1000L
            val latB = metricsTracker.getMetrics(keyB)?.averageLatencyMs ?: 1000L
            latA.compareTo(latB)
        }
    }

    fun prioritizeEndpoints(
        endpoints: List<EndpointEntity>,
        requiresVision: Boolean,
        requiresTools: Boolean
    ): List<EndpointEntity> {
        return prioritizeEndpointsForWorkload(endpoints, WorkloadType.CODING, requiresVision, requiresTools)
    }

    suspend fun healthCheckAll(endpoints: List<EndpointEntity>): Map<Int, ProviderHealthStatus> = coroutineScope {
        val results = mutableMapOf<Int, ProviderHealthStatus>()
        val deferreds = endpoints.map { endpoint ->
            async(Dispatchers.IO) {
                val provider = getProviderForEndpoint(endpoint)
                val config = object : EndpointConfig {
                    override val url = endpoint.url
                    override val apiKey = endpoint.apiKey
                    override val modelName = endpoint.modelName
                    override val providerType = endpoint.type
                }
                val status = provider.healthCheck(config)
                endpoint.id to status
            }
        }

        deferreds.awaitAll().forEach { (id, status) ->
            results[id] = status
        }
        results
    }

    /**
     * Builds real-time Intelligence Status Report for telemetry & user display
     */
    suspend fun getIntelligenceStatusReport(
        endpoints: List<EndpointEntity>,
        currentMissionName: String? = null,
        activeWorkload: WorkloadType? = WorkloadType.CODING
    ): IntelligenceStatusReport = coroutineScope {
        val entries = mutableListOf<ProviderStatusEntry>()

        val probeResults = endpoints.map { endpoint ->
            async(Dispatchers.IO) {
                val provider = getProviderForEndpoint(endpoint)
                val config = object : EndpointConfig {
                    override val url = endpoint.url
                    override val apiKey = endpoint.apiKey
                    override val modelName = endpoint.modelName
                    override val providerType = endpoint.type
                }
                val health = provider.healthCheck(config)
                val endpointKey = "${endpoint.type}:${endpoint.url}:${endpoint.modelName}"
                val rel = metricsTracker.getReliabilityScore(endpointKey)
                val cooldown = metricsTracker.getMetrics(endpointKey)?.cooldownUntilTimestamp?.let {
                    (it - System.currentTimeMillis()).coerceAtLeast(0L)
                } ?: 0L
                val bestWorkloads = workloadMatrix.getTopWorkloadsForProvider(endpoint.type)

                ProviderStatusEntry(
                    providerName = endpoint.name.ifBlank { endpoint.type },
                    providerType = endpoint.type,
                    modelName = endpoint.modelName,
                    status = health.status,
                    latencyMs = health.latencyMs,
                    reliabilityScore = rel,
                    activeCooldownRemainingMs = cooldown,
                    bestWorkloads = bestWorkloads,
                    detailMessage = health.message ?: "Operational"
                )
            }
        }.awaitAll()

        entries.addAll(probeResults)

        // Add Offline Fallback entry
        entries.add(
            ProviderStatusEntry(
                providerName = "Offline Deterministic Engine",
                providerType = "OFFLINE",
                modelName = "offline-ast",
                status = ProviderStatus.ONLINE,
                latencyMs = 1L,
                reliabilityScore = 1.0f,
                activeCooldownRemainingMs = 0L,
                bestWorkloads = listOf(WorkloadType.CODING),
                detailMessage = "Deterministic AST & local state preservation operational (Zero cognitive synthesis)"
            )
        )

        val targetWorkload = activeWorkload ?: WorkloadType.CODING
        val workerSelection = selectWorkerForWorkload(targetWorkload, endpoints)
        val selectedWorkerName = workerSelection.primaryEndpoint?.let {
            "${it.type} (${it.modelName})"
        } ?: "Offline Deterministic Engine"

        val fallbackChain = workerSelection.fallbackEndpoints.map { "${it.type} (${it.modelName})" } + listOf("Offline Deterministic Engine")

        IntelligenceStatusReport(
            providerEntries = entries,
            currentMission = currentMissionName,
            selectedWorker = selectedWorkerName,
            fallbackChain = fallbackChain,
            activeWorkload = targetWorkload,
            evidenceRequirement = "runtime + regression",
            estimatedCost = "$0.00",
            selectionReason = workerSelection.selectionReason
        )
    }

    fun formatIntelligenceStatus(report: IntelligenceStatusReport): String {
        val sb = StringBuilder()
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("🧠 M. ENGINE INTELLIGENCE CONTROL PLANE\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n")

        sb.append("PROVIDER STATUS:\n")
        report.providerEntries.forEach { entry ->
            val icon = when (entry.status) {
                ProviderStatus.ONLINE -> "🟢"
                ProviderStatus.RATE_LIMITED -> "🟡"
                ProviderStatus.QUOTA_EXHAUSTED -> "🔴"
                ProviderStatus.AUTH_FAILED -> "🚫"
                ProviderStatus.DEGRADED -> "⚠️"
                ProviderStatus.OFFLINE -> "⚪"
                ProviderStatus.UNCONFIGURED -> "🔘"
            }
            val cooldownStr = if (entry.activeCooldownRemainingMs > 0) " [Cooldown: ${entry.activeCooldownRemainingMs / 1000}s]" else ""
            val workloadStr = if (entry.bestWorkloads.isNotEmpty()) " (Best for: ${entry.bestWorkloads.joinToString(", ") { it.name }})" else ""
            sb.append("$icon ${entry.providerName} [${entry.modelName}] — ${entry.status}$cooldownStr | Latency: ${entry.latencyMs}ms | Reliability: ${"%.0f".format(entry.reliabilityScore * 100)}%$workloadStr\n")
        }

        sb.append("\n")
        if (report.currentMission != null) {
            sb.append("Current Mission: ${report.currentMission}\n")
        }
        sb.append("Active Workload: ${report.activeWorkload?.name ?: "GENERAL"}\n")
        sb.append("Selected Worker: ${report.selectedWorker}\n")
        sb.append("Fallback Chain: ${report.fallbackChain.joinToString(" → ")}\n")
        sb.append("Evidence Requirement: ${report.evidenceRequirement}\n")
        sb.append("Estimated Cost: ${report.estimatedCost}\n")
        sb.append("Selection Reason: ${report.selectionReason}\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        return sb.toString()
    }
}
