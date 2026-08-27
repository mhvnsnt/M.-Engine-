package com.example.ai

import android.util.Log
import com.example.data.EndpointEntity
import com.example.github.HierarchicalMemoryManager
import com.example.network.OllamaMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import org.json.JSONArray
import org.json.JSONObject

class AgentOrchestrator(
    private val memoryManager: HierarchicalMemoryManager,
    private val codeJarvis: CodeJarvis,
    private val codingTools: CodingTools
) {
    suspend fun plan(
        prompt: String,
        endpoints: List<EndpointEntity>,
        githubPat: String
    ): AgentPlan {
        if (!currentCoroutineContext().isActive) throw CancellationException("Agent generation cancelled")

        // 1. Retrieve Context
        val memoryContext = memoryManager.retrieveRelevantContext(prompt, topK = 3)

        // 2. Formulate Structured Planning Prompt
        val systemPrompt = """
            You are M. Engine, an autonomous agent orchestrator.
            Your goal is to understand the user's request, retrieve necessary context, and output a structured JSON plan.
            
            Available tools:
            - read_file (parameters: path)
            - write_file (parameters: path, content)
            - run_shell (parameters: command)
            - build_apk (parameters: none)
            - commit_and_push (parameters: message)

            Respond ONLY with a raw JSON object exactly matching this structure (no markdown, no backticks):
            {
              "goal": "Summary of what needs to be done",
              "steps": [
                {
                  "description": "Step explanation",
                  "toolName": "run_shell",
                  "parameters": { "command": "ls -la" }
                }
              ]
            }
            If no tool is needed, omit toolName and parameters or set them to null. Do not include markdown formatting.
        """.trimIndent()

        val history = listOf(
            OllamaMessage(
                role = "user", 
                content = "Context:\n$memoryContext\n\nRequest:\n$prompt"
            )
        )

        // 3. Call Model Router (reusing existing provider infrastructure via CodeJarvis)
        val rawResponse = codeJarvis.callModel(endpoints, systemPrompt, history)
        
        // 4. Safe Translation / Parsing
        return parsePlanSafely(rawResponse)
    }

    suspend fun executePlan(
        plan: AgentPlan,
        githubPat: String
    ): AgentResult {
        val results = mutableListOf<ToolResult>()
        var finalSummary = ""
        
        try {
            for (step in plan.steps) {
                currentCoroutineContext().ensureActive()
                if (step.toolRequest != null) {
                    val req = step.toolRequest
                    val output = executeTool(req, githubPat)
                    results.add(ToolResult(req, true, output))
                }
            }
            currentCoroutineContext().ensureActive()
            finalSummary = "Plan execution completed successfully. ${results.size} tools executed."
        } catch(e: CancellationException) {
            codingTools.cancelActiveProcesses()
            finalSummary = "OPERATION CANCELLED\nNo subsequent agent steps executed."
            throw e
        } catch(e: Exception) {
            finalSummary = "Error during execution: ${e.message}"
        }
        return AgentResult(plan, results, finalSummary)
    }

    private suspend fun executeTool(req: ToolRequest, githubPat: String): String {
        val owner = "mhvnsnt"
        val repo = "M.-Engine"
        val branch = "main"
        
        return when (req.toolName) {
            "read_file" -> {
                val path = req.parameters["path"] ?: return "Error: missing path"
                codingTools.readFile(githubPat, owner, repo, branch, path) ?: "File not found."
            }
            "write_file" -> {
                val path = req.parameters["path"] ?: return "Error: missing path"
                val content = req.parameters["content"] ?: return "Error: missing content"
                val success = codingTools.writeFile(githubPat, owner, repo, path, content)
                if (success) "Successfully wrote to $path" else "Failed to write to $path"
            }
            "run_shell" -> {
                val cmd = req.parameters["command"] ?: return "Error: missing command"
                codingTools.executeShell(cmd)
            }
            "execute_python" -> {
                val code = req.parameters["code"] ?: return "Error: missing code"
                codingTools.executePython(code)
            }
            "build_apk" -> {
                val (success, log) = codingTools.buildApk()
                log
            }
            "commit_and_push" -> {
                val msg = req.parameters["message"] ?: return "Error: missing message"
                val success = codingTools.commitAndPush(githubPat, owner, repo, branch, msg)
                if (success) "Successfully committed and pushed" else "Failed to commit and push"
            }
            else -> "Unknown tool: ${req.toolName}"
        }
    }

    private fun parsePlanSafely(rawText: String): AgentPlan {
        try {
            // Strip any accidental markdown formatting the model might include despite instructions
            var jsonString = rawText.trim()
            if (jsonString.startsWith("```json")) {
                jsonString = jsonString.substringAfter("```json")
            } else if (jsonString.startsWith("```")) {
                jsonString = jsonString.substringAfter("```")
            }
            if (jsonString.endsWith("```")) {
                jsonString = jsonString.substringBeforeLast("```")
            }
            jsonString = jsonString.trim()

            // If it doesn't look like JSON, wrap it in a safe fallback
            if (!jsonString.startsWith("{") || !jsonString.endsWith("}")) {
                throw Exception("Response is not a valid JSON object.")
            }

            val jsonObject = JSONObject(jsonString)
            val goal = jsonObject.optString("goal", "Execute user request")
            val stepsArray = jsonObject.optJSONArray("steps") ?: JSONArray()
            val steps = mutableListOf<AgentStep>()
            var requiresApproval = false

            for (i in 0 until stepsArray.length()) {
                val stepObj = stepsArray.getJSONObject(i)
                val desc = stepObj.optString("description", "")
                val toolName = stepObj.optString("toolName", "")
                var toolReq: ToolRequest? = null

                if (toolName.isNotEmpty() && toolName != "null") {
                    val paramsObj = stepObj.optJSONObject("parameters")
                    val params = mutableMapOf<String, String>()
                    if (paramsObj != null) {
                        paramsObj.keys().forEach { key ->
                            params[key] = paramsObj.getString(key)
                        }
                    }
                    val permission = determinePermission(toolName)
                    if (permission == PermissionLevel.HIGH_RISK_WRITE || 
                        permission == PermissionLevel.SYSTEM || 
                        permission == PermissionLevel.DESTRUCTIVE) {
                        requiresApproval = true
                    }
                    toolReq = ToolRequest(toolName, params, permission)
                }
                steps.add(AgentStep(desc, toolReq))
            }
            return AgentPlan(goal, steps, requiresApproval, rawText)
        } catch (e: Exception) {
            Log.e("AgentOrchestrator", "Failed to parse plan", e)
            // Fail safely: return a text-only plan with no tools to execute.
            return AgentPlan(
                goal = "Unable to generate structured plan. Model response was malformed.",
                steps = listOf(AgentStep(description = "Raw output: $rawText", toolRequest = null)),
                requiresApproval = false,
                rawResponse = rawText
            )
        }
    }

    private fun determinePermission(toolName: String): PermissionLevel {
        return when (toolName) {
            "read_file" -> PermissionLevel.READ
            "write_file", "commit_and_push" -> PermissionLevel.HIGH_RISK_WRITE
            "run_shell", "execute_python", "build_apk" -> PermissionLevel.SYSTEM
            else -> PermissionLevel.SYSTEM // Default to strictest if unknown tool
        }
    }
}
