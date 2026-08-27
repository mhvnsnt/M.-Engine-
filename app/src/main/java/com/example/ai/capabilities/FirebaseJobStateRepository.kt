package com.example.ai.capabilities

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log

class FirebaseJobStateRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) : JobStateRepository {

    override suspend fun updateJobState(jobId: String, status: String, resultMessage: String?): Boolean {
        return try {
            val update = mutableMapOf<String, Any>("status" to status)
            if (resultMessage != null) {
                update["resultMessage"] = resultMessage
            }
            db.collection("jobs").document(jobId).set(update, com.google.firebase.firestore.SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            Log.e("FirebaseJobStateRepository", "Failed to update job state: ${e.message}", e)
            false
        }
    }

    override suspend fun getJobState(jobId: String): String? {
        return try {
            val snapshot = db.collection("jobs").document(jobId).get().await()
            snapshot.getString("status")
        } catch (e: Exception) {
            Log.e("FirebaseJobStateRepository", "Failed to get job state: ${e.message}", e)
            null
        }
    }

    override suspend fun approveJob(jobId: String): Boolean {
        return try {
            val docRef = db.collection("jobs").document(jobId)
            var success = false
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentStatus = snapshot.getString("status")
                if (currentStatus == CognitiveState.WAITING_APPROVAL.name) {
                    transaction.update(docRef, "status", CognitiveState.DELEGATE.name)
                    success = true
                }
            }.await()
            success
        } catch (e: Exception) {
            Log.e("FirebaseJobStateRepository", "Failed to approve job: ${e.message}", e)
            false
        }
    }

    override suspend fun rejectJob(jobId: String): Boolean {
        return try {
            val docRef = db.collection("jobs").document(jobId)
            var success = false
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentStatus = snapshot.getString("status")
                if (currentStatus == CognitiveState.WAITING_APPROVAL.name) {
                    transaction.update(docRef, "status", CognitiveState.CANCELLED.name)
                    success = true
                }
            }.await()
            success
        } catch (e: Exception) {
            Log.e("FirebaseJobStateRepository", "Failed to reject job: ${e.message}", e)
            false
        }
    }
}
