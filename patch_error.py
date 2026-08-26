import re

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

ollama_error_old = """            } catch (e: Exception) {
                repository.updateMessage(placeholderMsg.copy(id = insertedId, text = "Network Error: ${e.message}"))
            }"""
ollama_error_new = """            } catch (e: Exception) {
                var errorMsg = e.message ?: "Unknown Error"
                if (endpoint.url.contains("10.0.2.2")) {
                    errorMsg += "\\n\\n(Fix: 10.0.2.2 only works in the Android Emulator. If you are on a real phone, go to Settings and change the URL to your computer's actual Wi-Fi IP address like 192.168.1.x, and ensure OLLAMA_HOST=0.0.0.0 is set on your PC before starting Ollama.)"
                }
                repository.updateMessage(placeholderMsg.copy(id = insertedId, text = "Network Error: $errorMsg"))
            }"""

content = content.replace(ollama_error_old, ollama_error_new)

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
