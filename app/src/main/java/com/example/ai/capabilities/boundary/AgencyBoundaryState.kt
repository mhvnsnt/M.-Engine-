package com.example.ai.capabilities.boundary

enum class AgencyBoundaryState {
    AUTHORIZED,
    PLANNING,
    ACTING,
    WAITING_FOR_EXTERNAL_CAPABILITY,
    OBSERVING,
    EVALUATING,
    CONTINUING,
    ESCALATING,
    HALTED,
    COMPLETED
}

data class AgencyBoundaryEvent(
    val state: AgencyBoundaryState,
    val description: String,
    val capabilityNeeded: String? = null,
    val providerAttempted: String? = null
)

class AgencyBoundaryStateMachine {
    var currentState: AgencyBoundaryState = AgencyBoundaryState.AUTHORIZED
        private set
        
    private val history = mutableListOf<AgencyBoundaryEvent>()
    
    fun transition(event: AgencyBoundaryEvent): Boolean {
        // Log the transition
        history.add(event)
        
        // Ensure valid state progression (simplified for now)
        currentState = event.state
        return true
    }
    
    fun getHistory(): List<AgencyBoundaryEvent> = history.toList()
}
