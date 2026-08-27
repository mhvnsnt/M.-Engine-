package com.example.ai.capabilities

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class Phase11IntegrationTest {

    class ReproducingAppActuator : AppActuator {
        override suspend fun launch(packageName: String) = true
        override suspend fun tap(x: Int, y: Int) = true
        override suspend fun inputText(text: String) = true
        override suspend fun swipe(startX: Float, startY: Float, endX: Float, endY: Float) = true
        override suspend fun pressBack() = true
        override suspend fun observe() = ScreenObservation(0, "mock", "mock")
        override suspend fun recordVideo(durationMs: Long, outputPath: String) = true
        override suspend fun dumpUi(outputPath: String) = "mock ui tree"
        override suspend fun captureSession(durationMs: Long, actions: List<InteractionEvent>): VideoSessionTrace {
            return VideoSessionTrace(durationMs, "video.mp4", emptyList())
        }
        override suspend fun terminate(packageName: String) {}
    }

    @Test
    fun testMandatoryReproductionGate() = runBlocking {
        val engine = MultimodalVerificationEngineImpl()
        val actuator = ReproducingAppActuator()
        
        val loop = SelfHealingLoopImpl(
            EvidenceAssuranceEngineImpl(),
            Phase9IntegrationTest.MockGitHubService(),
            Phase10IntegrationTest.MockCiCd(),
            engine,
            RegressionEngineImpl(),
            actuator
        )
        
        val issue = DiscoveredIssue(
            id = "issue-404",
            category = IssueCategory.RUNTIME,
            severity = IssueSeverity.HIGH,
            description = "Unreproducible bug",
            context = "Crash",
            repositoryRef = RepositoryRef("owner", "repo")
        )
        
        val result = loop.heal(issue)
        
        // Because the mock actuator returns "App behaved as expected", it fails the reproduction gate!
        assertEquals(HealingStatus.REVERTED_NO_REPRODUCTION, result.status)
    }
}
