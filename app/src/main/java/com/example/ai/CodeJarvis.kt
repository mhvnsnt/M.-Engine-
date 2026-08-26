package com.example.ai

import android.util.Log
import com.example.data.EndpointEntity
import com.example.data.GraphNode
import com.example.data.GraphNodeDao
import com.example.network.OllamaChatRequest
import com.example.network.OllamaMessage
import com.example.network.OpenRouterContentPart
import com.example.network.OpenRouterMessage
import com.example.network.OpenRouterRequest
import com.example.network.RetrofitClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class CodeJarvis(
    private val codingTools: CodingTools,
    private val treeSitterEngine: TreeSitterEngine,
    private val graphDao: GraphNodeDao
) {
    suspend fun handleCodeCommand(
        command: String,
        githubPat: String,
        owner: String = "mhvnsnt",
        repo: String = "M.-Engine",
        branch: String = "main",
        endpoint: EndpointEntity,
        telegramBotToken: String = "",
        telegramChatId: Long = 0L
    ): String = withContext(Dispatchers.IO) {
        // Mem0 Episodic Deduplication Logic
        try {
            val recentEpisodes = graphDao.getActiveNodesByType("EPISODIC")
            if (recentEpisodes.size > 5) {
                val summaryText = "User executed ${recentEpisodes.size} commands today focusing on ${recentEpisodes.map { it.obj }.distinct().take(2).joinToString()}. Compressed."
                recentEpisodes.forEach { graphDao.invalidateNode(it.id) }
                graphDao.insert(GraphNode(
                    subject = "User",
                    predicate = "summarized_activity",
                    obj = summaryText,
                    type = "ARCHIVAL"
                ))
                Log.d("CodeJarvis", "Mem0: Compressed overlapping commands into ARCHIVAL timeline summary.")
            }
            graphDao.insert(GraphNode(
                subject = "User",
                predicate = "ran_command",
                obj = command.take(50),
                type = "EPISODIC"
            ))
        } catch (e: Exception) {
            Log.e("CodeJarvis", "Error in Mem0 graph memory", e)
        }

        try {
            val systemInstruction = """
                You are CodeJarvis, a highly autonomous agentic coding engine embedded inside M. Engine.
                You operate on a continuous Gather Context -> Take Action -> Verify Results loop.
                You have direct native access to the Android terminal, Python 3 runtime, and GitHub repository.

                Available tools:
                - list_files()
                - read_file(path)
                - edit_file(path, search_string, replace_string)
                - unified_diff(path, diff_content)
                - run_shell(command)
                - run_python(code)
                - build_apk()
                - deploy_apk(chat_id)
                - commit_and_push(message)
                - finish(final_response_to_user)

                Formatting instructions:
                TOOL: list_files

                TOOL: read_file
                PATH: app/src/main/java/com/example/MainActivity.kt

                TOOL: edit_file
                PATH: app/src/main/java/com/example/MainActivity.kt
                SEARCH: old_code
                REPLACE: new_code

                TOOL: run_shell
                COMMAND: gradle test

                TOOL: run_python
                CODE:
                import sys
                print("Python version:", sys.version)

                TOOL: build_apk

                TOOL: deploy_apk
                CHAT_ID: 123456789

                TOOL: commit_and_push
                MESSAGE: feat: update build logic

                TOOL: finish
                MESSAGE: Task complete!
            """.trimIndent()

            val currentPrompt = "The user wants you to do the following: $command"
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
                if (responseText.contains("TOOL: list_files")) {
                    val files = codingTools.listFiles(githubPat, owner, repo, branch)
                    toolOutput = files.joinToString("\n")
                } else if (responseText.contains("TOOL: read_file")) {
                    val path = responseText.substringAfter("PATH:").trim().split("\n")[0].trim()
                    val content = codingTools.readFile(githubPat, owner, repo, branch, path)
                    toolOutput = content ?: "File not found: $path"
                } else if (responseText.contains("TOOL: run_python")) {
                    val pythonCode = responseText.substringAfter("CODE:").trim()
                    toolOutput = codingTools.executePython(pythonCode)
                } else if (responseText.contains("TOOL: run_shell")) {
                    val cmd = responseText.substringAfter("COMMAND:").trim().split("\n")[0].trim()
                    toolOutput = codingTools.executeShell(cmd)
                } else if (responseText.contains("TOOL: build_apk")) {
                    val (success, log) = codingTools.buildApk()
                    toolOutput = log
                } else if (responseText.contains("TOOL: deploy_apk")) {
                    val targetChatIdStr = responseText.substringAfter("CHAT_ID:").trim().split("\n")[0].trim()
                    val targetChatId = targetChatIdStr.toLongOrNull() ?: telegramChatId
                    toolOutput = codingTools.deployApkToTelegram(telegramBotToken, targetChatId)
                } else if (responseText.contains("TOOL: edit_file")) {
                    val path = responseText.substringAfter("PATH:").substringBefore("SEARCH:").trim()
                    val searchStr = responseText.substringAfter("SEARCH:").substringBefore("REPLACE:").trim()
                    val replaceStr = responseText.substringAfter("REPLACE:").substringBefore("TOOL:").trim()

                    val currentContent = codingTools.readFile(githubPat, owner, repo, branch, path)
                    toolOutput = if (currentContent != null) {
                        if (currentContent.contains(searchStr)) {
                            val newContent = currentContent.replace(searchStr, replaceStr)
                            codingTools.writeFile(githubPat, owner, repo, path, newContent)
                            "Successfully edited $path locally."
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
                            val newContent = patchedLines.joinToString("\n")

                            codingTools.writeFile(githubPat, owner, repo, path, newContent)
                            "Successfully applied diff to $path locally."
                        } catch (e: Exception) {
                            "Failed to apply diff to $path: ${e.message}"
                        }
                    } else {
                        "Failed to read $path for patching."
                    }
                } else if (responseText.contains("TOOL: commit_and_push")) {
                    val msg = responseText.substringAfter("MESSAGE:").trim().split("\n")[0].trim()
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
                    break
                } else {
                    toolOutput = "Error: Unrecognized tool or format. Make sure you use exactly TOOL: <tool_name> and required parameters."
                }

                history.add(OllamaMessage(role = "user", content = "TOOL RESULT:\n$toolOutput\n\nWhat is your next action?"))
            }

            if (iteration >= maxIterations && finalResult.isEmpty()) {
                finalResult = "Agentic loop finished."
            }
            return@withContext finalResult
        } catch (e: Exception) {
            Log.e("CodeJarvis", "Code error", e)
            return@withContext "Error executing agentic loop: ${e.message}"
        }
    }

    private suspend fun callModel(endpoint: EndpointEntity, systemPrompt: String, history: List<OllamaMessage>): String {
        return if (endpoint.type == "OPENAI") {
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
            if (!response.isSuccessful) throw Exception("HTTP ${response.code()}: ${response.errorBody()?.string()}")
            val reader = BufferedReader(InputStreamReader(response.body()!!.byteStream()))
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
                            val c = resp?.choices?.firstOrNull()
                            (c?.delta?.content ?: c?.message?.content)?.let { completeResponse += it }
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
            if (!response.isSuccessful) throw Exception("HTTP ${response.code()}: ${response.errorBody()?.string()}")
            val reader = BufferedReader(InputStreamReader(response.body()!!.byteStream()))
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
