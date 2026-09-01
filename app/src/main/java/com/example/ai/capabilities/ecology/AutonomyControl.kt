package com.example.ai.capabilities.ecology

enum class AutonomyState {
    AUTONOMY_ENABLED,
    AUTONOMY_PAUSED,
    EMERGENCY_STOP
}

object AutonomyControlPlane {
    var currentState: AutonomyState = AutonomyState.AUTONOMY_ENABLED
    
    fun isExecutionAllowed(): Boolean {
        return currentState == AutonomyState.AUTONOMY_ENABLED
    }

    fun triggerEmergencyStop() {
        currentState = AutonomyState.EMERGENCY_STOP
    }

    fun resumeExecution() {
        currentState = AutonomyState.AUTONOMY_ENABLED
    }

    fun pauseExecution() {
        currentState = AutonomyState.AUTONOMY_PAUSED
    }
}
