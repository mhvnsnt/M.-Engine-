import re

with open("app/src/main/java/com/example/ai/CodeJarvis.kt", "r") as f:
    content = f.read()

# We will just rewrite the file fully to implement the loop.
# But wait, it's easier to just generate a new file.

new_content = """package com.example.ai

import android.util.Log
import com.example.data.EndpointEntity
import com.example.network.OllamaChatRequest
import com.example.network.OllamaMessage
import com.example.network.OpenRouterContentPart
import com.example.network.OpenRouterMessage
import com.example.network.OpenRouterRequest
import com.example.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class CodeJarvis(
    private val codingTools: CodingTools,
    private val treeSitterEngine: TreeSitterEngine
) {
    suspend fun handleCodeCommand(
        command: String,
        githubPat: String,
        owner: String = "mhvnsnt",
        repo: String = "M.-Engine",
        branch: String = "main",
        endpoint: EndpointEntity
    ): String = withContext(Dispatchers.IO) {
        if (githubPat.isEmpty()) {
            return@withContext "Error: GitHub PAT is required for coding capabilities."
        }
        
        try {
            val systemInstruction = \"\"\"
                You are CodeJarvis, a highly autonomous agentic coding engine embedded inside M. Engine.
                You operate on a continuous Gather Context -> Take Action -> Verify Results loop.
                You have direct access to the local Android environment and the GitHub repository.
                
                Available tools (Simulated via structured output):
                - list_files()
                - read_file(path)
                - edit_file(path, search_string, replace_string)
                - unified_diff(path, diff_content)
                - run_shell(command)
                - commit_and_push(message)
                - finish(final_response_to_user)
                
                You must output ONLY one tool command at a time in this exact format:
                
                TOOL: read_file
                PATH: app/src/main/java/com/example/MainActivity.kt
                
                or
                
                TOOL: run_shell
                COMMAND: ls -la
                
                or
                
                TOOL: finish
                MESSAGE: I have completed the task and pushed the code.
                
                IMPORTANT: You are in an agentic loop. When you use a tool (except finish), the system will run it and give you the output. You must keep using tools until the task is complete, then call finish.
            \"\"\".trimIndent()

            var currentPrompt = "The user wants you to do the following: $command"
            var iteration = 0
            val maxIterations = 15
            val history = mutableListOf<OllamaMessage>()
            history.add(OllamaMessage(role = "user", content = currentPrompt))
            
            var finalResult = ""

            while (iteration < maxIterations) {
                iteration++
                Log.d("CodeJarvis", "Agent Loop Iteration: $iteration")
                
                val responseText = callModel(endpoint, systemInstruction, history)
                history.add(OllamaMessage(role = "assistant", content = responseText))
                
                var toolOutput = ""
                
                if (responseText.contains("TOOL: read_file")) {
                    val path = responseText.substringAfter("PATH:").trim().split("\\n")[0].trim()
                    val content = codingTools.readFile(githubPat, owner, repo, branch, path)
                    toolOutput = if (content != null) {
                        val astInfo = treeSitterEngine.parseAST(content, "kotlin")
                        "Successfully read $path.\\n\\n$astInfo\\n\\n```kotlin\\n$content\\n```"
                    } else {
                        "Failed to read $path."
                    }
                } else if (responseText.contains("TOOL: list_files")) {
                    val files = codingTools.listFiles(githubPat, owner, repo, branch)
                    toolOutput = "Repository files:\\n" + files.joinToString("\\n")
                } else if (responseText.contains("TOOL: run_shell")) {
                    val shellCmd = responseText.substringAfter("COMMAND:").trim().split("\\n")[0].trim()
                    toolOutput = try {
                        val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", shellCmd))
                        val reader = BufferedReader(InputStreamReader(process.inputStream))
                        val errorReader = BufferedReader(InputStreamReader(process.errorStream))
                        var out = ""
                        var err = ""
                        var line: String?
                        while (reader.readLine().also { line = it } != null) { out += line + "\\n" }
                        while (errorReader.readLine().also { line = it } != null) { err += line + "\\n" }
                        process.waitFor()
                        "STDOUT:\\n$out\\nSTDERR:\\n$err"
                    } catch (e: Exception) {
                        "Shell Execution Error: ${e.message}"
                    }
                } else if (responseText.contains("TOOL: edit_file")) {
                    val path = responseText.substringAfter("PATH:").substringBefore("SEARCH:").trim()
                    val searchStr = responseText.substringAfter("SEARCH:").substringBefore("REPLACE:").trim()
                    val replaceStr = responseText.substringAfter("REPLACE:").substringBefore("TOOL:").trim()
                    
                    val currentContent = codingTools.readFile(githubPat, owner, repo, branch, path)
                    toolOutput = if (currentContent != null) {
                        if (currentContent.contains(searchStr)) {
                            val newContent = currentContent.replace(searchStr, replaceStr)
                            codingTools.writeFile(githubPat, owner, repo, path, newContent)
                            "Successfully edited $path locally. Note: Changes are NOT pushed yet. Use commit_and_push when done."
                        } else {
                            "Failed to edit $path: Search string not found in the file."
                        }
                    } else {
                        "Failed to read $path for editing."
                    }
                } else if (responseText.contains("TOOL: unified_diff")) {
                    val path = responseText.substringAfter("PATH:").substringBefore("DIFF:").trim()
                    val diffContent = responseText.substringAfter("DIFF:").substringBefore("TOOL:").trim()
                    
                    val currentContent = codingTools.readFile(githubPat, owner, repo, branch, path)
                    toolOutput = if (currentContent != null) {
                        try {
                            val patch = com.github.difflib.UnifiedDiffUtils.parseUnifiedDiff(diffContent.lines())
                            val originalLines = currentContent.lines()
                            val patchedLines = com.github.difflib.DiffUtils.patch(originalLines, patch)
                            val newContent = patchedLines.joinToString("\\n")
                            
                            codingTools.writeFile(githubPat, owner, repo, path, newContent)
                            "Successfully applied diff to $path locally."
                        } catch (e: Exception) {
                            "Failed to apply diff to $path: ${e.message}"
                        }
                    } else {
                        "Failed to read $path for patching."
                    }
                } else if (responseText.contains("TOOL: commit_and_push")) {
                    val msg = responseText.substringAfter("MESSAGE:").trim().split("\\n")[0].trim()
                    val success = codingTools.commitAndPush(
                        pat = githubPat,
                        owner = owner,
                        repo = repo,
                        branch = branch,
                        message = msg
                    )
                    toolOutput = if (success) {
                        "Successfully committed and pushed to GitHub with message: $msg"
                    } else {
                        "Failed to commit and push to GitHub."
                    }
                } else if (responseText.contains("TOOL: finish")) {
                    val msg = responseText.substringAfter("MESSAGE:").trim()
                    finalResult = msg
                    break // Exit the loop!
                } else {
                    toolOutput = "Error: Unrecognized tool or format. Make sure you use exactly TOOL: <tool_name> and the required parameters."
                }
                
                // Add the tool result back into the history for the next iteration
                history.add(OllamaMessage(role = "user", content = "TOOL RESULT:\\n$toolOutput\\n\\nWhat is your next action?"))
            }
            
            if (iteration >= maxIterations && finalResult.isEmpty()) {
                finalResult = "Agentic loop timed out after $maxIterations iterations."
            }

            return@withContext finalResult
        } catch (e: Exception) {
            Log.e("CodeJarvis", "Code error", e)
            return@withContext "Error executing agentic loop: ${e.message}"
        }
    }

    private suspend fun callModel(endpoint: EndpointEntity, systemPrompt: String, history: List<OllamaMessage>): String {
        return if (endpoint.url.contains("openrouter")) {
            val openRouterHistory = mutableListOf<OpenRouterMessage>()
            openRouterHistory.add(OpenRouterMessage(role = "system", content = listOf(OpenRouterContentPart(type = "text", text = systemPrompt))))
            history.forEach { msg ->
                openRouterHistory.add(OpenRouterMessage(role = msg.role, content = listOf(OpenRouterContentPart(type = "text", text = msg.content))))
            }
            
            val req = OpenRouterRequest(
                model = endpoint.modelName,
                messages = openRouterHistory,
                stream = false
            )
            val response = RetrofitClient.openRouterService.generateChatStream(endpoint.url, "Bearer ${endpoint.apiKey}", request = req)
            val reader = BufferedReader(InputStreamReader(response.byteStream()))
            var completeResponse = ""
            var line: String?
            val adapter = Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter(com.example.network.OpenRouterResponse::class.java)
            while (reader.readLine().also { line = it } != null) {
                line?.let { jsonLine ->
                    if (jsonLine.startsWith("data: ")) {
                        val data = jsonLine.substring(6)
                        if (data != "[DONE]") {
                            try {
                                val chunk = adapter.fromJson(data)
                                chunk?.choices?.firstOrNull()?.delta?.content?.let { completeResponse += it }
                            } catch (e: Exception) {}
                        }
                    } else if (jsonLine.startsWith("{")) {
                        try {
                            val resp = adapter.fromJson(jsonLine)
                            resp?.choices?.firstOrNull()?.delta?.content?.let { completeResponse += it }
                        } catch (e: Exception) {}
                    }
                }
            }
            completeResponse
        } else {
            val ollamaHistory = mutableListOf<OllamaMessage>()
            ollamaHistory.add(OllamaMessage(role = "system", content = systemPrompt))
            ollamaHistory.addAll(history)
            
            val req = OllamaChatRequest(
                model = endpoint.modelName,
                messages = ollamaHistory,
                stream = false
            )
            val response = RetrofitClient.service.generateChatStream(endpoint.url, req)
            val reader = BufferedReader(InputStreamReader(response.byteStream()))
            var completeResponse = ""
            var line: String?
            val adapter = Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter(com.example.network.OllamaChatResponse::class.java)
            while (reader.readLine().also { line = it } != null) {
                line?.let { jsonLine ->
                    try {
                        val chunk = adapter.fromJson(jsonLine)
                        chunk?.message?.content?.let { completeResponse += it }
                    } catch (e: Exception) {}
                }
            }
            completeResponse
        }
    }
}
"""

with open("app/src/main/java/com/example/ai/CodeJarvis.kt", "w") as f:
    f.write(new_content)
