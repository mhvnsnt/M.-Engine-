package com.example.ai.capabilities

/**
 * Interface defining the boundary for durable job state synchronization.
 * In production, this syncs with Firebase Firestore/Cloud Functions.
 */
interface JobStateRepository {
    /**
     * Updates the job state durably on the server.
     */
    suspend fun updateJobState(jobId: String, status: String, resultMessage: String? = null): Boolean

    /**
     * Fetches the current authoritative status of the job from the server.
     * Useful for recovery after process death.
     */
    suspend fun getJobState(jobId: String): String?
    
    /**
     * Approves a pending job (WAITING_APPROVAL -> DELEGATE).
     * This is server-authoritative.
     */
    suspend fun approveJob(jobId: String): Boolean

    /**
     * Rejects a pending job (WAITING_APPROVAL -> CANCELLED).
     */
    suspend fun rejectJob(jobId: String): Boolean
}

class FirebaseJobStateRepositoryMock : JobStateRepository {
    private val durableStore = mutableMapOf<String, String>()

    override suspend fun updateJobState(jobId: String, status: String, resultMessage: String?): Boolean {
        durableStore[jobId] = status
        return true
    }

    override suspend fun getJobState(jobId: String): String? {
        return durableStore[jobId]
    }

    override suspend fun approveJob(jobId: String): Boolean {
        val current = durableStore[jobId]
        if (current == CognitiveState.WAITING_APPROVAL.name) {
            durableStore[jobId] = CognitiveState.DELEGATE.name
            return true
        }
        return false
    }

    override suspend fun rejectJob(jobId: String): Boolean {
        val current = durableStore[jobId]
        if (current == CognitiveState.WAITING_APPROVAL.name) {
            durableStore[jobId] = CognitiveState.CANCELLED.name
            return true
        }
        return false
    }
}
