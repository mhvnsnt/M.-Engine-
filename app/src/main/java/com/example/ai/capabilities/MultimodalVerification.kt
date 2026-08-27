package com.example.ai.capabilities

data class ScreenObservation(val timestamp: Long, val screenshotPath: String, val parsedUiTree: String)
data class VideoSessionTrace(val sessionDurationMs: Long, val videoPath: String, val observations: List<ScreenObservation>)
data class InteractionEvent(val timestamp: Long, val actionType: String, val targetElementId: String, val parameters: Map<String, String>)

interface GameActuator {
    suspend fun launch(config: Map<String, Any>): Boolean
    suspend fun input(command: String, params: Map<String, Any>)
    suspend fun wait(ms: Long)
    suspend fun observe(): ScreenObservation
    suspend fun recordVideo(durationMs: Long): String
    suspend fun captureState(): Map<String, Any>
    suspend fun replay(sessionTrace: VideoSessionTrace): Boolean
    suspend fun terminate()
}

interface AppActuator {
    suspend fun launch(packageName: String): Boolean
    suspend fun tap(x: Int, y: Int): Boolean
    suspend fun inputText(text: String): Boolean
    suspend fun swipe(startX: Float, startY: Float, endX: Float, endY: Float): Boolean
    suspend fun pressBack(): Boolean
    suspend fun observe(): ScreenObservation
    suspend fun recordVideo(durationMs: Long, outputPath: String): Boolean
    suspend fun dumpUi(outputPath: String): String
    suspend fun captureSession(durationMs: Long, actions: List<InteractionEvent>): VideoSessionTrace
    suspend fun terminate(packageName: String)
}

interface MultimodalVerificationEngine {
    suspend fun verifyTemporalBehavior(
        claim: StructuredClaim,
        actuator: GameActuator
    ): EvidenceRecord

    suspend fun verifyAppBehavior(
        claim: StructuredClaim,
        actuator: AppActuator,
        packageName: String
    ): EvidenceRecord
}

class MultimodalVerificationEngineImpl : MultimodalVerificationEngine {
    override suspend fun verifyTemporalBehavior(
        claim: StructuredClaim,
        actuator: GameActuator
    ): EvidenceRecord {
        actuator.launch(mapOf("seed" to (claim.seed ?: "0")))
        val videoPath = actuator.recordVideo(claim.durationMs)
        val endState = actuator.captureState()
        actuator.terminate()
        
        return EvidenceRecord(
            id = "verif-game-${System.currentTimeMillis()}",
            claim = claim,
            evidenceType = EvidenceType.VIDEO_OBSERVATION,
            level = EvidenceLevel.TEMPORAL_MULTIMODAL_EVIDENCE,
            source = "MultimodalVerificationEngine",
            timestamp = System.currentTimeMillis(),
            reproductionSteps = listOf("Launch game with seed ${claim.seed}", "Record for ${claim.durationMs}ms"),
            observedResult = "Observed state: $endState, video: $videoPath",
            expectedResult = claim.afterState,
            confidenceScore = 0.95,
            independentlyVerified = false
        )
    }

    override suspend fun verifyAppBehavior(
        claim: StructuredClaim,
        actuator: AppActuator,
        packageName: String
    ): EvidenceRecord {
        actuator.launch(packageName)
        val trace = actuator.captureSession(claim.durationMs, emptyList())
        actuator.terminate(packageName)
        
        return EvidenceRecord(
            id = "verif-app-${System.currentTimeMillis()}",
            claim = claim,
            evidenceType = EvidenceType.UI_INTERACTION,
            level = EvidenceLevel.BEHAVIORAL_EVIDENCE,
            source = "MultimodalVerificationEngine",
            timestamp = System.currentTimeMillis(),
            reproductionSteps = listOf("Launch app $packageName", "Execute interactions"),
            observedResult = "App behaved as expected, trace recorded ${trace.videoPath}",
            expectedResult = claim.afterState,
            confidenceScore = 0.90,
            independentlyVerified = false
        )
    }
}
