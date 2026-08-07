import re

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'r') as f:
    content = f.read()

old_ollama = """                        try {
                            val chunk = responseAdapter.fromJson(jsonLine)
                            chunk?.message?.content?.let { contentChunk ->
                                completeResponse += contentChunk
                                val now = System.currentTimeMillis()
                                if (now - lastUpdateTime > 50 || chunk.done) {
                                    repository.updateMessage(placeholderMsg.copy(id = insertedId, text = completeResponse))
                                    lastUpdateTime = now
                                }
                            }
                        } catch (e: Exception) { }"""
new_ollama = """                        try {
                            val chunk = responseAdapter.fromJson(jsonLine)
                            chunk?.message?.content?.let { contentChunk ->
                                completeResponse += contentChunk
                                ttsBuffer += contentChunk
                                if (ttsBuffer.contains(Regex("[.!?\\n]")) || (ttsBuffer.contains(" ") && ttsBuffer.length > 30)) {
                                    ttsEngine.speak(ttsBuffer, flush = false)
                                    ttsBuffer = ""
                                }
                                val now = System.currentTimeMillis()
                                if (now - lastUpdateTime > 50 || chunk.done) {
                                    repository.updateMessage(placeholderMsg.copy(id = insertedId, text = completeResponse))
                                    lastUpdateTime = now
                                }
                            }
                        } catch (e: Exception) { }"""

# Make sure we only add ttsBuffer once if we already replaced it above!
# But streamOllamaModel does NOT have `var ttsBuffer = ""` yet.
# Actually, the string replacement above replaced `var completeResponse = ""` with `var completeResponse = ""\n var ttsBuffer = ""` globally!
# Let's check if it replaced both.

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'w') as f:
    content = content.replace(old_ollama, new_ollama)
    f.write(content)
