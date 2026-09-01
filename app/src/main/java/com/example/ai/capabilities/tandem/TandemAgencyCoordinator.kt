package com.example.ai.capabilities.tandem

import com.example.ai.capabilities.AgencyLedger
import com.example.ai.capabilities.OpportunityEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface TandemAgencyCoordinator {
    fun onUserMessageReceived(message: String)
    fun processBackgroundQueue()
    fun getObservatory(): AgencyObservatory
}

class TandemAgencyCoordinatorImpl(
    private val signalMiner: SignalMiner = SignalMinerImpl(),
    private val observatory: AgencyObservatory = AgencyObservatoryImpl(),
    private val ledger: AgencyLedger,
    private val opportunityEngine: OpportunityEngine,
    private val maxAutonomyLevel: AutonomyGradient = AutonomyGradient.PROPOSE
) : TandemAgencyCoordinator {

    private val backgroundQueue = mutableListOf<DevelopmentSignal>()

    override fun onUserMessageReceived(message: String) {
        // 1. Human Conversation Stream -> Extract Signals
        val signals = signalMiner.mineConversation(message)
        
        for (signal in signals) {
            backgroundQueue.add(signal)
            
            // Broadcast to the transparent Mindstream
            observatory.broadcast(MindstreamEntry(
                mission = "Signal Classification",
                currentState = "QUEUED",
                objective = "Process user intent",
                whyThisMatters = "Priority: ${signal.priority}",
                currentAction = "Extracting signal: ${signal.type}",
                decision = "Added to background work queue."
            ))
        }
        
        // Asynchronously trigger background processing (simulated)
        CoroutineScope(Dispatchers.Default).launch {
            processBackgroundQueue()
        }
    }

    override fun processBackgroundQueue() {
        if (backgroundQueue.isEmpty()) return
        
        val target = backgroundQueue.removeAt(0)
        
        observatory.broadcast(MindstreamEntry(
            mission = "Background Development",
            currentState = "RESEARCHING",
            objective = target.description,
            whyThisMatters = "Derived from user conversation.",
            currentAction = "Comparing against existing architecture..."
        ))
        
        // Check Autonomy Gradient
        if (maxAutonomyLevel.level >= AutonomyGradient.EXPERIMENT.level) {
            observatory.broadcast(MindstreamEntry(
                mission = "Background Development",
                currentState = "EXPERIMENTING",
                objective = target.description,
                whyThisMatters = "Testing feasibility.",
                currentAction = "Testing in sandbox..."
            ))
        }
        
        if (maxAutonomyLevel.level < AutonomyGradient.IMPLEMENT.level) {
            observatory.broadcast(MindstreamEntry(
                mission = "Background Development",
                currentState = "AWAITING_AUTHORIZATION",
                objective = target.description,
                whyThisMatters = "Requires implementation.",
                currentAction = "Proposing architectural implementation plan.",
                decision = "Do not integrate. Awaiting Owner Approval.",
                learning = "Strategy mapped, waiting on human authorization layer."
            ))
        }
    }
    
    override fun getObservatory(): AgencyObservatory = observatory
}
