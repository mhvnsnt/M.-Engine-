import re

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

jarvis_logic = """
            if (text.startsWith("/code ")) {
                val command = text.removePrefix("/code ").trim()
                val responseMsg = MessageEntity(text = "Executing CodeJarvis...", isUser = false, responderName = "CodeJarvis", groupId = groupId)
                val insertedId = repository.insertMessage(responseMsg).toInt()
                
                try {
                    val primaryEndpoint = repository.getPrimaryEndpoint()
                    if (primaryEndpoint != null) {
                        val result = codeJarvis.handleCodeCommand(
                            command = command,
                            githubPat = githubPat.value,
                            endpoint = primaryEndpoint
                        )
                        repository.updateMessage(responseMsg.copy(id = insertedId, text = result))
                    } else {
                        repository.updateMessage(responseMsg.copy(id = insertedId, text = "Error: No primary endpoint selected for CodeJarvis."))
                    }
                } catch(e: Exception) {
                    repository.updateMessage(responseMsg.copy(id = insertedId, text = "CodeJarvis Error: ${e.message}"))
                }
                syncMemory()
                return@launch
            }
"""

content = content.replace("val userMsg = MessageEntity(text = text, isUser = true, groupId = groupId, imageUri = imageUri)", jarvis_logic + "\n            val userMsg = MessageEntity(text = text, isUser = true, groupId = groupId, imageUri = imageUri)")

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
