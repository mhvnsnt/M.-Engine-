package com.example.ai.capabilities

enum class WorkloadType {
    CODING,
    DEBUGGING,
    REPOSITORY_COMPREHENSION,
    UI_REASONING,
    VIDEO_MULTIMODAL,
    TOOL_USE,
    LONG_CONTEXT,
    PLANNING,
    RESEARCH,
    SELF_CORRECTION
}

data class WorkloadBenchmarkScore(
    val workload: WorkloadType,
    val providerType: String,
    val modelName: String,
    val score: Float, // 0.0 to 1.0
    val sampleCount: Int = 1,
    val averageLatencyMs: Long = 500L
)

class WorkloadBenchmarkMatrix {
    private val matrix = mutableMapOf<String, WorkloadBenchmarkScore>()

    init {
        // Seed initial evidence-based baselines across workload categories
        // Claude 3.5 Sonnet / Direct Anthropic
        recordBaseline(WorkloadType.CODING, "ANTHROPIC", "claude-3-5-sonnet", 0.98f, 250L)
        recordBaseline(WorkloadType.DEBUGGING, "ANTHROPIC", "claude-3-5-sonnet", 0.96f, 300L)
        recordBaseline(WorkloadType.PLANNING, "ANTHROPIC", "claude-3-5-sonnet", 0.95f, 280L)
        recordBaseline(WorkloadType.SELF_CORRECTION, "ANTHROPIC", "claude-3-5-sonnet", 0.97f, 310L)
        recordBaseline(WorkloadType.REPOSITORY_COMPREHENSION, "ANTHROPIC", "claude-3-5-sonnet", 0.94f, 400L)

        // Gemini 3.5 Flash / Gemini Pro Direct
        recordBaseline(WorkloadType.LONG_CONTEXT, "GEMINI", "gemini-3.5-flash", 0.99f, 180L)
        recordBaseline(WorkloadType.RESEARCH, "GEMINI", "gemini-3.5-flash", 0.96f, 200L)
        recordBaseline(WorkloadType.TOOL_USE, "GEMINI", "gemini-3.5-flash", 0.94f, 210L)
        recordBaseline(WorkloadType.UI_REASONING, "GEMINI", "gemini-3.5-flash", 0.93f, 220L)
        recordBaseline(WorkloadType.VIDEO_MULTIMODAL, "GEMINI", "gemini-3.5-flash", 0.97f, 250L)
        recordBaseline(WorkloadType.CODING, "GEMINI", "gemini-3.5-flash", 0.91f, 190L)

        // OpenRouter Universal
        recordBaseline(WorkloadType.CODING, "OPENROUTER", "openai/gpt-4o", 0.94f, 450L)
        recordBaseline(WorkloadType.RESEARCH, "OPENROUTER", "openai/gpt-4o", 0.93f, 420L)
        recordBaseline(WorkloadType.TOOL_USE, "OPENROUTER", "openai/gpt-4o", 0.95f, 400L)
        recordBaseline(WorkloadType.UI_REASONING, "OPENROUTER", "openai/gpt-4o", 0.92f, 460L)

        // Ollama Local / On-Device (Fast, Private, Zero Cloud Cost)
        recordBaseline(WorkloadType.CODING, "OLLAMA", "deepseek-coder-v2", 0.89f, 90L)
        recordBaseline(WorkloadType.DEBUGGING, "OLLAMA", "deepseek-coder-v2", 0.88f, 95L)
        recordBaseline(WorkloadType.REPOSITORY_COMPREHENSION, "OLLAMA", "qwen2.5-coder", 0.87f, 110L)

        // OpenAI Compatible Universal (Groq, Together, DeepSeek Direct)
        recordBaseline(WorkloadType.CODING, "OPENAI", "deepseek-chat", 0.96f, 320L)
        recordBaseline(WorkloadType.DEBUGGING, "OPENAI", "deepseek-chat", 0.95f, 340L)
        recordBaseline(WorkloadType.SELF_CORRECTION, "OPENAI", "deepseek-chat", 0.94f, 350L)
    }

    private fun recordBaseline(workload: WorkloadType, providerType: String, modelName: String, score: Float, latencyMs: Long) {
        val key = makeKey(workload, providerType, modelName)
        matrix[key] = WorkloadBenchmarkScore(workload, providerType, modelName, score, 1, latencyMs)
    }

    fun recordEmpiricalResult(workload: WorkloadType, providerType: String, modelName: String, passed: Boolean, latencyMs: Long) {
        val key = makeKey(workload, providerType, modelName)
        val existing = matrix[key]
        if (existing == null) {
            matrix[key] = WorkloadBenchmarkScore(
                workload = workload,
                providerType = providerType,
                modelName = modelName,
                score = if (passed) 0.80f else 0.40f,
                sampleCount = 1,
                averageLatencyMs = latencyMs
            )
        } else {
            val newCount = existing.sampleCount + 1
            val newScore = if (passed) {
                (existing.score * existing.sampleCount + 1.0f) / newCount
            } else {
                (existing.score * existing.sampleCount + 0.0f) / newCount
            }.coerceIn(0.0f, 1.0f)

            val newLatency = (existing.averageLatencyMs * existing.sampleCount + latencyMs) / newCount
            matrix[key] = existing.copy(score = newScore, sampleCount = newCount, averageLatencyMs = newLatency)
        }
    }

    fun getScore(workload: WorkloadType, providerType: String, modelName: String): Float {
        val key = makeKey(workload, providerType, modelName)
        return matrix[key]?.score ?: when (providerType.uppercase()) {
            "ANTHROPIC", "CLAUDE" -> 0.88f
            "GEMINI" -> 0.85f
            "OPENROUTER" -> 0.82f
            "OPENAI" -> 0.84f
            "OLLAMA" -> 0.75f
            "OFFLINE" -> 0.10f
            else -> 0.70f
        }
    }

    fun getTopWorkloadsForProvider(providerType: String): List<WorkloadType> {
        return matrix.values
            .filter { it.providerType.equals(providerType, ignoreCase = true) }
            .sortedByDescending { it.score }
            .map { it.workload }
            .distinct()
            .take(3)
    }

    private fun makeKey(workload: WorkloadType, providerType: String, modelName: String): String {
        return "${workload.name}:${providerType.uppercase()}:${modelName.lowercase()}"
    }
}
