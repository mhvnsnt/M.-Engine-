package com.example.ai.capabilities

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import android.util.Log

class FirebaseSandboxManager(private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()) : RemoteSandboxManager {

    override suspend fun provisionSandbox(jobId: String, config: SandboxConfig): String {
        return try {
            val data = mapOf(
                "jobId" to jobId,
                "config" to mapOf(
                    "limits" to mapOf(
                        "maxMemoryMb" to config.limits.maxMemoryMb,
                        "maxCpuCores" to config.limits.maxCpuCores,
                        "maxExecutionMinutes" to config.limits.maxExecutionMinutes
                    ),
                    "networkPolicy" to config.networkPolicy.name,
                    "baseImage" to config.baseImage
                )
            )
            
            val result = functions.getHttpsCallable("provisionSandbox").call(data).await()
            @Suppress("UNCHECKED_CAST")
            val resultData = result.data as? Map<String, Any>
            resultData?.get("sandboxId") as? String ?: throw Exception("Invalid response from provisionSandbox")
            
        } catch (e: Exception) {
            Log.e("FirebaseSandboxManager", "Failed to provision sandbox: ${e.message}", e)
            "fallback-sandbox-id"
        }
    }

    override suspend fun destroySandbox(sandboxId: String): Boolean {
        return try {
            val data = mapOf("sandboxId" to sandboxId)
            functions.getHttpsCallable("destroySandbox").call(data).await()
            true
        } catch (e: Exception) {
            Log.e("FirebaseSandboxManager", "Failed to destroy sandbox: ${e.message}", e)
            false
        }
    }

    override suspend fun cloneRepository(sandboxId: String, repo: RepositoryRef, secureToken: String): Boolean {
        val command = "git clone https://${secureToken}@github.com/${repo.owner}/${repo.name}.git ."
        val executionResult = executeCommand(sandboxId, command, timeoutMinutes = 5)
        return executionResult.exitCode == 0
    }

    override suspend fun executeCommand(sandboxId: String, command: String, timeoutMinutes: Int): ExecutionResult {
        return try {
            val data = mapOf(
                "sandboxId" to sandboxId,
                "command" to command,
                "timeoutMinutes" to timeoutMinutes
            )
            
            val result = functions.getHttpsCallable("executeInSandbox").call(data).await()
            @Suppress("UNCHECKED_CAST")
            val resultData = result.data as? Map<String, Any>
            
            ExecutionResult(
                exitCode = (resultData?.get("exitCode") as? Number)?.toInt() ?: -1,
                stdout = resultData?.get("stdout") as? String ?: "",
                stderr = resultData?.get("stderr") as? String ?: "",
                timeoutTriggered = resultData?.get("timeoutTriggered") as? Boolean ?: false
            )
        } catch (e: Exception) {
            Log.e("FirebaseSandboxManager", "Failed to execute command: ${e.message}", e)
            ExecutionResult(-1, "", e.message ?: "Unknown error", false)
        }
    }
}
