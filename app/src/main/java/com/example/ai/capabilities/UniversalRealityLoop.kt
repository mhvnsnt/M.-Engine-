package com.example.ai.capabilities

enum class LoopStage {
    UNDERSTAND, RETRIEVE, RESEARCH, PLAN, RISK_ASSESSMENT, 
    IMPLEMENT, BUILD, RUN, OBSERVE, REPRODUCE, DIAGNOSE, 
    FIX, RETEST, COMPARE, GATHER_EVIDENCE, CHECK_REGRESSION, 
    REVIEW, DELIVER
}

enum class FailureType {
    STATIC_UI,
    VISUAL_GLITCH,
    BEHAVIORAL_BUG,
    GAMEPLAY_ISSUE,
    PERFORMANCE_BOTTLENECK,
    AUDIO_ISSUE,
    NETWORK_FAILURE,
    CRASH
}

data class ObservationStrategy(
    val requiredMode: ObservationMode,
    val actuatorType: String,
    val requiresTemporalRecording: Boolean
)

interface ObservationRouter {
    fun selectStrategy(failureType: FailureType): ObservationStrategy
}

class DefaultObservationRouter : ObservationRouter {
    override fun selectStrategy(failureType: FailureType): ObservationStrategy {
        return when(failureType) {
            FailureType.STATIC_UI -> ObservationStrategy(ObservationMode.UI_TREE, "ui_inspector", false)
            FailureType.VISUAL_GLITCH -> ObservationStrategy(ObservationMode.SCREEN, "screenshot_actuator", false)
            FailureType.BEHAVIORAL_BUG -> ObservationStrategy(ObservationMode.VIDEO, "session_recorder", true)
            FailureType.GAMEPLAY_ISSUE -> ObservationStrategy(ObservationMode.GAME_STATE, "game_actuator", true)
            FailureType.PERFORMANCE_BOTTLENECK -> ObservationStrategy(ObservationMode.FPS_PERFORMANCE, "profiler", true)
            FailureType.AUDIO_ISSUE -> ObservationStrategy(ObservationMode.AUDIO, "audio_capture", true)
            FailureType.NETWORK_FAILURE -> ObservationStrategy(ObservationMode.NETWORK, "network_tracer", true)
            FailureType.CRASH -> ObservationStrategy(ObservationMode.CRASH_LOG, "logcat_monitor", false)
        }
    }
}

interface UniversalRealityLoop {
    suspend fun executeStage(stage: LoopStage, context: MissionContext): Boolean
    suspend fun runFullPipeline(mission: Mission): Boolean
}
