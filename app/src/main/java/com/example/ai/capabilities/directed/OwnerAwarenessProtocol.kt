package com.example.ai.capabilities.directed

enum class AwarenessThreshold {
    PASSIVE,   // Acts without interrupting (research, monitoring, sandbox)
    NOTIFY,    // Tells you (discovered opportunity, completed experiment)
    APPROVAL   // Must wait (merge code, spend money, external publish)
}

interface OwnerAwarenessProtocol {
    fun determineThreshold(actionType: String, risk: Double, reversibility: Double): AwarenessThreshold
    fun executeUnderProtocol(actionType: String, risk: Double, reversibility: Double, action: () -> Unit)
}

class OwnerAwarenessProtocolImpl : OwnerAwarenessProtocol {
    override fun determineThreshold(actionType: String, risk: Double, reversibility: Double): AwarenessThreshold {
        // Lower reversibility or high risk means more owner awareness needed
        return when {
            reversibility < 0.3 || risk > 0.8 -> AwarenessThreshold.APPROVAL
            risk > 0.4 || reversibility < 0.7 -> AwarenessThreshold.NOTIFY
            else -> AwarenessThreshold.PASSIVE
        }
    }

    override fun executeUnderProtocol(actionType: String, risk: Double, reversibility: Double, action: () -> Unit) {
        val threshold = determineThreshold(actionType, risk, reversibility)
        
        when (threshold) {
            AwarenessThreshold.PASSIVE -> {
                // Execute quietly in background
                action()
            }
            AwarenessThreshold.NOTIFY -> {
                // Notify owner, then execute (or notify upon completion)
                // In a real impl, this would emit an event to the UI
                action()
            }
            AwarenessThreshold.APPROVAL -> {
                // Pause and wait for owner approval
                // action() is NOT executed immediately
            }
        }
    }
}
