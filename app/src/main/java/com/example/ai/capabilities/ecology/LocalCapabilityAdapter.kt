package com.example.ai.capabilities.ecology

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Local Fallback Preservation
 * Preserves the ability to execute Android-specific local capabilities,
 * without competing with the canonical remote ledger.
 */
object LocalCapabilityAdapter {
    private val repository = RemoteControlPlaneRepository.shared

    suspend fun evaluateAndExecuteLocal(wakeRecord: MetabolismWakeRecord): Boolean = withContext(Dispatchers.IO) {
        // Evaluate connection to remote
        repository.refreshState()
        
        val currentState = repository.connectionState.value
        if (currentState == RemoteGovernorState.CONNECTED) {
            println("OBSERVED: REMOTE_GOVERNOR_ACTIVE. Local edge is IDLE. Yielding autonomous execution to Remote.")
            RuntimeObservatory.currentActivity = "REMOTE_GOVERNOR_ACTIVE - EDGE IDLE"
            // Return false to indicate no local work was done (remote handles it)
            return@withContext false
        } else {
            println("OBSERVED: REMOTE_GOVERNOR_UNAVAILABLE. LOCAL_FALLBACK_EVALUATING.")
            RuntimeObservatory.currentActivity = "LOCAL_FALLBACK_EVALUATING"
            
            // Here, we explicitly label that local execution happened.
            // When connection restores, EvidenceReconciliationEngine should sync this to remote.
            
            val budget = ExecutionBudget(maxIterations = 2) // Restricted fallback budget
            val capabilities = listOf(
                GitHubWorkerCapability(),
                SandboxExecutionCapability(),
                DocumentationCapability()
            )
            val loop = AutonomousExecutionLoop(budget, capabilities)
            
            val loopResult = loop.run(wakeRecord)
            
            println("RESULT: LOCAL_FALLBACK Execution complete. Work will be synced when remote returns.")
            return@withContext true
        }
    }
}
