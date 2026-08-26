package com.example.ai

import android.util.Log
import com.example.data.EndpointEntity
import com.example.network.RetrofitClient
import com.example.network.TelegramMessageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LindyEngine(
    private val telegramBotTokenFlow: Flow<String>,
    private val codeJarvis: CodeJarvis,
    private val githubPatFlow: Flow<String>,
    private val codingTools: CodingTools
) {
    private var lastUpdateId: Long = 0

    fun startProactiveLoop(getPrimaryEndpoint: suspend () -> EndpointEntity?) {
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                try {
                    val token = telegramBotTokenFlow.first()
                    if (token.isNotEmpty()) {
                        pollTelegram(token, getPrimaryEndpoint)
                    }
                } catch (e: Exception) {
                    Log.e("LindyEngine", "Error in Lindy polling loop", e)
                }
                delay(3000) // Poll every 3 seconds for free Telegram updates
            }
        }
    }

    private suspend fun pollTelegram(token: String, getPrimaryEndpoint: suspend () -> EndpointEntity?) {
        try {
            val response = RetrofitClient.telegramService.getUpdates(token, offset = lastUpdateId + 1, timeout = 10)
            if (response.ok && response.result != null) {
                for (update in response.result) {
                    lastUpdateId = update.update_id
                    val msg = update.message
                    if (msg != null && msg.text != null) {
                        val text = msg.text
                        Log.d("LindyEngine", "Received Telegram message: $text")
                        val chatId = msg.chat.id

                        val endpoint = getPrimaryEndpoint()
                        val githubPat = githubPatFlow.first()

                        if (text.startsWith("/deploy") || text.startsWith("/build") || text.startsWith("/apk")) {
                            RetrofitClient.telegramService.sendMessage(
                                token,
                                TelegramMessageRequest(chatId, "🛠️ Building & Deploying APK via Lindy Protocol...")
                            )
                            val deployResult = codingTools.deployApkToTelegram(token, chatId, "M Engine Automated Build")
                            RetrofitClient.telegramService.sendMessage(
                                token,
                                TelegramMessageRequest(chatId, deployResult)
                            )
                        } else if (text.startsWith("/python")) {
                            val code = text.removePrefix("/python").trim()
                            RetrofitClient.telegramService.sendMessage(
                                token,
                                TelegramMessageRequest(chatId, "🐍 Executing Python code...")
                            )
                            val pyResult = codingTools.executePython(code)
                            RetrofitClient.telegramService.sendMessage(
                                token,
                                TelegramMessageRequest(chatId, pyResult)
                            )
                        } else {
                            // Acknowledge receipt
                            RetrofitClient.telegramService.sendMessage(
                                token,
                                TelegramMessageRequest(chatId, "Thinking... (Lindy Protocol)")
                            )

                            if (endpoint != null) {
                                val reply = if (text.startsWith("/code")) {
                                    codeJarvis.handleCodeCommand(
                                        command = text.removePrefix("/code").trim(),
                                        githubPat = githubPat,
                                        endpoint = endpoint,
                                        telegramBotToken = token,
                                        telegramChatId = chatId
                                    )
                                } else {
                                    codeJarvis.handleCodeCommand(
                                        command = text,
                                        githubPat = githubPat,
                                        endpoint = endpoint,
                                        telegramBotToken = token,
                                        telegramChatId = chatId
                                    )
                                }

                                val chunks = reply.chunked(4000)
                                for (chunk in chunks) {
                                    RetrofitClient.telegramService.sendMessage(
                                        token,
                                        TelegramMessageRequest(chatId, chunk)
                                    )
                                }
                            } else {
                                RetrofitClient.telegramService.sendMessage(
                                    token,
                                    TelegramMessageRequest(chatId, "Error: No primary endpoint configured in M Engine.")
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore timeout errors
        }
    }
}
