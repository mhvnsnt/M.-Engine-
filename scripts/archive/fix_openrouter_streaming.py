import re

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'r') as f:
    content = f.read()

# For OpenRouter:
old_or = """                                val chunk = openRouterResponseAdapter.fromJson(jsonLine)
                                chunk?.choices?.firstOrNull()?.delta?.content?.let { contentChunk ->
                                    completeResponse += contentChunk
                                    val now = System.currentTimeMillis()
                                    if (now - lastUpdateTime > 50) {
                                        repository.updateMessage(placeholderMsg.copy(id = insertedId, text = completeResponse))
                                        lastUpdateTime = now
                                    }
                                }"""
new_or = """                                val chunk = openRouterResponseAdapter.fromJson(jsonLine)
                                chunk?.choices?.firstOrNull()?.delta?.content?.let { contentChunk ->
                                    completeResponse += contentChunk
                                    ttsBuffer += contentChunk
                                    // Speak if we hit punctuation or a newline
                                    if (ttsBuffer.contains(Regex("[.!?\\n]")) || (ttsBuffer.contains(" ") && ttsBuffer.length > 30)) {
                                        ttsEngine.speak(ttsBuffer, flush = false)
                                        ttsBuffer = ""
                                    }
                                    val now = System.currentTimeMillis()
                                    if (now - lastUpdateTime > 50) {
                                        repository.updateMessage(placeholderMsg.copy(id = insertedId, text = completeResponse))
                                        lastUpdateTime = now
                                    }
                                }"""
content = content.replace(old_or, new_or)

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'w') as f:
    f.write(content)
