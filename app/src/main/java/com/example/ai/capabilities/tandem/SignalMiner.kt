package com.example.ai.capabilities.tandem

interface SignalMiner {
    fun mineConversation(message: String): List<DevelopmentSignal>
}

class SignalMinerImpl : SignalMiner {
    override fun mineConversation(message: String): List<DevelopmentSignal> {
        val signals = mutableListOf<DevelopmentSignal>()
        
        // Simulating the extraction of intent from user messages
        if (message.contains("research everything daily", ignoreCase = true)) {
            signals.add(DevelopmentSignal(
                type = SignalType.NEW_REQUIREMENT,
                description = "Continuous epistemic maintenance",
                priority = 0.9
            ))
        } else if (message.contains("never do", ignoreCase = true) || message.contains("don't", ignoreCase = true)) {
             signals.add(DevelopmentSignal(
                type = SignalType.CORRECTION,
                description = "Extracting boundary correction from user",
                priority = 1.0
            ))
        } else {
             // General observation
             signals.add(DevelopmentSignal(
                type = SignalType.ARCHITECTURE_IMPROVEMENT,
                description = "Analyze user conversation for latent structural improvements",
                priority = 0.3
            ))
        }
        return signals
    }
}
