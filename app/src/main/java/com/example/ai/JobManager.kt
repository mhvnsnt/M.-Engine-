package com.example.ai

import android.content.Context
import android.util.Log
import com.example.data.JobDao
import com.example.data.JobEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class JobManager(
    private val context: Context,
    private val jobDao: JobDao,
    private val orchestrator: AgentOrchestrator,
    private val codingTools: CodingTools,
    private val githubPat: String
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    val activeJobOutput = MutableStateFlow<String>("")

    fun startJob(jobId: Long, prompt: String, endpoints: List<com.example.data.EndpointEntity>) {
        scope.launch {
            try {
                val job = jobDao.getJob(jobId) ?: return@launch
                
                // --- DURABLE RECOVERY & GITHUB SYNC (PHASE 13) ---
                var currentLogs = job.logs
                currentLogs += "\n[Job Recovery] Authenticating and syncing with remote source of truth..."
                jobDao.updateJob(job.copy(status = "RECOVERING", logs = currentLogs))
                activeJobOutput.value = currentLogs
                
                val owner = "mhvnsnt"
                val repo = "M.-Engine"
                val branch = job.branch.takeIf { it.isNotBlank() } ?: "main"
                
                // 1. Fetch Repository
                val fetchSuccess = codingTools.fetchAndPull(githubPat, owner, repo)
                if (!fetchSuccess) {
                    currentLogs += "\n[Job Recovery] FAILED to fetch repository. Worker disconnected or GitHub unavailable."
                    jobDao.updateJob(jobDao.getJob(jobId)!!.copy(status = "UNAVAILABLE", logs = currentLogs))
                    activeJobOutput.value = currentLogs
                    return@launch
                }
                
                // 2. Checkout Target Branch
                codingTools.checkoutBranch(githubPat, owner, repo, branch)
                
                // 3. Verify Commit
                val headSha = codingTools.getCommitSha(owner, repo)
                currentLogs += "\n[Job Recovery] Sync complete. HEAD is at $headSha on branch $branch"
                jobDao.updateJob(jobDao.getJob(jobId)!!.copy(status = "RUNNING", currentCommit = headSha, logs = currentLogs))
                activeJobOutput.value = currentLogs

                // --- MAIN LOOP ---
                var loopCount = job.currentCycle
                var maxLoops = 15
                var isComplete = false
                var currentContext = "User Request: $prompt\n"
                
                while (loopCount < maxLoops && !isComplete && isActive) {
                    loopCount++
                    currentLogs += "\n\n--- [LOOP $loopCount] Generating Plan ---"
                    jobDao.updateJob(jobDao.getJob(jobId)!!.copy(currentCycle = loopCount, logs = currentLogs))
                    activeJobOutput.value = currentLogs

                    // 1. Generate Plan based on current context
                    val plan = orchestrator.plan("Current Context:\n$currentContext\n\nBased on the context, what is the next step to fulfill the request? If the request is fully completed and verified, use no tools and state that the goal is complete.", endpoints, githubPat)
                    
                    currentLogs += "\nPlan generated: ${plan.goal}"
                    jobDao.updateJob(jobDao.getJob(jobId)!!.copy(logs = currentLogs))
                    activeJobOutput.value = currentLogs
                    
                    if (plan.steps.isEmpty()) {
                        isComplete = true
                        currentLogs += "\n\nNo further steps planned. Marking as completed."
                        break
                    }

                    // 2. Execute Plan
                    val result = orchestrator.executePlan(plan, githubPat)
                    
                    currentLogs += "\n\nExecution finished:\n" + result.finalSummary
                    jobDao.updateJob(jobDao.getJob(jobId)!!.copy(logs = currentLogs))
                    activeJobOutput.value = currentLogs
                    
                    // Accumulate context for next loop
                    currentContext += "\nAction Taken: ${plan.goal}\nResult:\n${result.finalSummary}\n"
                    
                    if (currentContext.length > 20000) {
                        currentContext = currentContext.substring(currentContext.length - 20000)
                    }
                    
                    delay(2000)
                }
                
                val finalStatus = if (isComplete) "COMPLETED" else "FAILED (Max loops reached or error)"
                currentLogs += "\n\nJob $finalStatus."
                jobDao.updateJob(jobDao.getJob(jobId)!!.copy(status = if(isComplete) "COMPLETED" else "FAILED", logs = currentLogs))
                activeJobOutput.value = currentLogs

            } catch (e: Exception) {
                Log.e("JobManager", "Job failed", e)
                val currentLogs = jobDao.getJob(jobId)?.logs ?: ""
                val newLogs = currentLogs + "\n\nFAILED: ${e.message}"
                jobDao.updateJob(jobDao.getJob(jobId)!!.copy(status = "FAILED", logs = newLogs))
                activeJobOutput.value = newLogs
            }
        }
    }
}
