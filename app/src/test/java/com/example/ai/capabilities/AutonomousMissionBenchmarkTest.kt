package com.example.ai.capabilities

import com.example.data.EndpointEntity
import com.example.data.MissionDao
import com.example.data.MissionEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AutonomousMissionBenchmarkTest {

    private class InMemoryMissionDao : MissionDao {
        val missions = mutableMapOf<String, MissionEntity>()

        override suspend fun insertMission(mission: MissionEntity) {
            missions[mission.id] = mission
        }

        override suspend fun updateMission(mission: MissionEntity) {
            missions[mission.id] = mission
        }

        override suspend fun getMission(id: String): MissionEntity? {
            return missions[id]
        }

        override suspend fun getAllMissions(): List<MissionEntity> {
            return missions.values.toList()
        }
    }

    /**
     * Test Provider that can be configured to succeed or throw simulated rate limits/outages
     */
    private class TestControllableProvider(
        override val name: String,
        override val providerId: String,
        var shouldFailWithRateLimit: Boolean = false
    ) : ModelProvider {
        override val type = CapabilityType.MODEL
        override val isLocal = false
        override val status = CapabilityStatus.ONLINE
        override val permissionLevel = com.example.ai.PermissionLevel.READ
        override val supportedOperations = listOf("generate")
        override val networkRequired = true
        override val modelCapabilities = ModelCapabilities(
            supportsStreaming = true,
            supportsImages = true,
            supportsTools = true,
            supportsJsonSchema = true,
            contextWindowLength = 32000,
            maxOutputTokens = 4096,
            speedTier = SpeedTier.FAST,
            costTier = CostTier.LOW
        )

        override suspend fun healthCheck(config: EndpointConfig): ProviderHealthStatus {
            return ProviderHealthStatus(
                status = ProviderStatus.ONLINE,
                latencyMs = 50L,
                message = "Ready"
            )
        }

        override suspend fun generate(request: ModelRequest): ModelResponse {
            if (shouldFailWithRateLimit) {
                throw RuntimeException("HTTP 429: Rate limit reached. Please retry after 30 seconds.")
            }
            return ModelResponse(
                text = "Reasoning result from $name for stage execution.",
                modelUsed = request.endpointConfig.modelName,
                providerUsed = name,
                latencyMs = 45L,
                finishReason = "stop"
            )
        }

        override suspend fun stream(request: ModelRequest) = kotlinx.coroutines.flow.flow {
            emit(ModelStream(chunk = "Chunk from $name", providerUsed = name, isComplete = true))
        }
    }

    private lateinit var missionDao: InMemoryMissionDao
    private lateinit var contextEngine: PersonalContextEngine
    private lateinit var evidenceEngine: EvidenceAssuranceEngine
    private lateinit var regressionEngine: RegressionEngine
    private lateinit var primaryProvider: TestControllableProvider
    private lateinit var secondaryProvider: TestControllableProvider
    private lateinit var modelRouter: ModelRouter
    private lateinit var missionEngine: MissionEngine
    private lateinit var realityLoop: UniversalRealityLoopImpl
    private lateinit var benchmark: AutonomousSelfImprovementBenchmark

    @Before
    fun setUp() {
        missionDao = InMemoryMissionDao()
        contextEngine = PersonalContextEngineImpl()
        evidenceEngine = EvidenceAssuranceEngineImpl()
        regressionEngine = RegressionEngineImpl()

        primaryProvider = TestControllableProvider("PrimaryWorker", "GEMINI")
        secondaryProvider = TestControllableProvider("SecondaryWorker", "ANTHROPIC")

        val registry = CapabilityRegistryImpl().apply {
            register(primaryProvider)
            register(secondaryProvider)
            register(OfflineFallbackProvider())
        }

        modelRouter = ModelRouter(registry)
        missionEngine = MissionEngineImpl(missionDao)
        realityLoop = UniversalRealityLoopImpl(
            modelRouter = modelRouter,
            missionEngine = missionEngine,
            evidenceEngine = evidenceEngine,
            personalContextEngine = contextEngine
        )

        benchmark = AutonomousSelfImprovementBenchmark(
            modelRouter = modelRouter,
            missionEngine = missionEngine,
            realityLoop = realityLoop,
            evidenceEngine = evidenceEngine,
            contextEngine = contextEngine,
            regressionEngine = regressionEngine
        )
    }

    @Test
    fun testRealityLoop_SurvivesPrimaryProviderFailureViaInFlightFailover() = runBlocking {
        // Configure two endpoints: Primary (Gemini) and Secondary (Anthropic)
        val primaryEndpoint = EndpointEntity(
            id = 1,
            name = "Primary Gemini",
            type = "GEMINI",
            url = "https://generativelanguage.googleapis.com",
            apiKey = "key1",
            modelName = "gemini-3.5-flash",
            isPrimary = true,
            isActive = true
        )
        val secondaryEndpoint = EndpointEntity(
            id = 2,
            name = "Secondary Anthropic",
            type = "ANTHROPIC",
            url = "https://api.anthropic.com",
            apiKey = "key2",
            modelName = "claude-3-5-sonnet",
            isPrimary = false,
            isActive = true
        )

        val endpoints = listOf(primaryEndpoint, secondaryEndpoint)

        // Make primary fail with HTTP 429 Rate Limit
        primaryProvider.shouldFailWithRateLimit = true

        val mission = missionEngine.createMission("Self-Improvement Resilience Test", contextEngine)

        // Run full pipeline
        val success = realityLoop.runFullPipelineWithEndpoints(mission, endpoints)

        assertTrue("Reality loop must succeed by falling over to secondary provider", success)

        val savedMission = missionEngine.getMission(mission.id)
        assertNotNull(savedMission)
        assertEquals(MissionStatus.ACHIEVED, savedMission?.currentState)

        // Verify provider switch was logged in mission history
        val history = savedMission?.history.orEmpty()
        assertTrue(
            "Must contain failover event in history",
            history.any { it.contains("PROVIDER_FAILOVER") || it.contains("Failover") }
        )
    }

    @Test
    fun testRealityLoop_StrictRealityContractWhenAllProvidersOffline() = runBlocking {
        // No remote endpoints configured (only OfflineFallback available)
        val mission = missionEngine.createMission("Complex Architecture Synthesis", contextEngine)

        val success = realityLoop.runFullPipelineWithEndpoints(mission, emptyList())

        assertFalse("Reality loop must NOT declare success when all cognitive providers are offline", success)

        val savedMission = missionEngine.getMission(mission.id)
        assertEquals(
            "Mission must be paused at BLOCKED_WAITING_PROVIDER",
            MissionStatus.BLOCKED_WAITING_PROVIDER,
            savedMission?.currentState
        )

        // Verify offline fallback preserved checkpoint
        val checkpoint = modelRouter.fallbackProvider.getCheckpoint(mission.id)
        assertNotNull(checkpoint)
    }

    @Test
    fun testSelfImprovementBenchmark_EndToEndMissionExecution() = runBlocking {
        val claudeEndpoint = EndpointEntity(
            id = 1,
            name = "Claude Direct",
            type = "ANTHROPIC",
            url = "https://api.anthropic.com",
            apiKey = "valid-key",
            modelName = "claude-3-5-sonnet",
            isPrimary = true,
            isActive = true
        )

        val result = benchmark.executeSelfImprovementMission(
            targetRepo = "mhvnsnt/M.-Engine-",
            endpoints = listOf(claudeEndpoint),
            simulateProviderFailureMidMission = false
        )

        assertTrue(result.isSuccess)
        assertEquals("mhvnsnt/M.-Engine-", result.targetRepo)
        assertEquals(18, result.stagesCompleted)
        assertNotNull(result.evidenceRecordId)

        // Verify evidence is present in EvidenceAssuranceEngine
        val evidenceClaim = evidenceEngine.evaluateClaim("Provider Independence & Failover Resilience on mhvnsnt/M.-Engine-")
        assertTrue(evidenceClaim)
    }
}
