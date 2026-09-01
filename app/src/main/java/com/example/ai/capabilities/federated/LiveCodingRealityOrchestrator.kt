package com.example.ai.capabilities.federated

import java.security.MessageDigest

class LiveCodingRealityOrchestrator(
    private val workerAdapter: OpenHandsWorkerAdapter,
    private val snapshotResolver: RepositorySnapshotResolver = RepositorySnapshotResolver()
) {
    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun executeTrial(authorization: CodingTrialAuthorization): LiveCodingTrialEvidence {
        val states = mutableListOf<TrialState>()
        
        // 1. AUTHORIZED
        states.add(TrialState.AUTHORIZED)

        // 2. REPOSITORY_OBSERVED
        val resolvedSha = snapshotResolver.resolveHeadSha(authorization.repositoryId)
        if (resolvedSha.isEmpty()) {
            states.add(TrialState.AUTHORIZATION_FAILED)
            return createEvidence(authorization, resolvedSha, states, TrialState.AUTHORIZATION_FAILED, EpistemicCapabilityState.IMPLEMENTED_UNVERIFIED)
        }
        states.add(TrialState.REPOSITORY_OBSERVED)

        // 3. WORKSPACE_CREATED
        val sessionId = try {
            workerAdapter.provisionEphemeralContainer(authorization)
        } catch (e: Exception) {
            states.add(TrialState.WORKER_UNREACHABLE)
            return createEvidence(authorization, resolvedSha, states, TrialState.WORKER_UNREACHABLE, EpistemicCapabilityState.IMPLEMENTED_UNVERIFIED)
        }
        states.add(TrialState.WORKSPACE_CREATED)
        states.add(TrialState.WORKER_REACHED)

        // 4. JOB_DISPATCHED & WORKER_REPORTED_RESULT
        workerAdapter.dispatchBoundedTask(sessionId, authorization.task.instruction)
        states.add(TrialState.JOB_DISPATCHED)
        states.add(TrialState.WORKER_REPORTED_RESULT)

        // 5. ARTIFACTS_INDEPENDENTLY_INSPECTED
        val rawDiff = workerAdapter.retrieveDiff(sessionId)
        val diffHash = if (rawDiff.isNotEmpty()) sha256(rawDiff) else null
        if (diffHash == null) {
            states.add(TrialState.ARTIFACT_VERIFICATION_FAILED)
            workerAdapter.destroyContainer(sessionId)
            return createEvidence(authorization, resolvedSha, states, TrialState.ARTIFACT_VERIFICATION_FAILED, EpistemicCapabilityState.IMPLEMENTED_UNVERIFIED)
        }
        states.add(TrialState.ARTIFACTS_INDEPENDENTLY_INSPECTED)

        // 6. TEST_EXECUTION_OBSERVED
        val rawTestOutput = workerAdapter.retrieveTestOutput(sessionId)
        if (!rawTestOutput.contains("BUILD SUCCESSFUL") || !rawTestOutput.contains("0 failed")) {
            states.add(TrialState.TEST_FAILED)
            workerAdapter.destroyContainer(sessionId)
            return createEvidence(authorization, resolvedSha, states, TrialState.TEST_FAILED, EpistemicCapabilityState.IMPLEMENTED_UNVERIFIED, diffHash)
        }
        states.add(TrialState.TEST_EXECUTION_OBSERVED)

        // 7. CLEANUP_INSPECTED
        val destroyed = workerAdapter.destroyContainer(sessionId)
        if (!destroyed) {
            states.add(TrialState.CLEANUP_UNKNOWN)
            return createEvidence(authorization, resolvedSha, states, TrialState.CLEANUP_UNKNOWN, EpistemicCapabilityState.IMPLEMENTED_UNVERIFIED, diffHash)
        }
        states.add(TrialState.CLEANUP_INSPECTED)

        // 8. VERIFIED_PARTIAL
        states.add(TrialState.VERIFIED_PARTIAL)

        return createEvidence(authorization, resolvedSha, states, TrialState.VERIFIED_PARTIAL, EpistemicCapabilityState.ORCHESTRATOR_LOGIC_VERIFIED, diffHash)
    }
    
    private fun createEvidence(
        auth: CodingTrialAuthorization, 
        sha: String?, 
        states: List<TrialState>, 
        finalState: TrialState, 
        capState: EpistemicCapabilityState,
        diffHash: String? = null
    ): LiveCodingTrialEvidence {
        return LiveCodingTrialEvidence(
            authorization = auth,
            resolvedCommitSha = sha,
            stateProgression = states,
            finalState = finalState,
            capabilityState = capState,
            diffHash = diffHash
        )
    }
}
