import re

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

target1 = "    private suspend fun streamOpenRouterModel(endpoint: EndpointEntity, history: List<OllamaMessage>, groupId: Long) {"
new1 = "    private suspend fun streamOpenRouterModel(endpoint: EndpointEntity, history: List<OllamaMessage>, groupId: Long): Boolean {"
content = content.replace(target1, new1)

target2 = "    private suspend fun streamOllamaModel(endpoint: EndpointEntity, history: List<OllamaMessage>, groupId: Long) {"
new2 = "    private suspend fun streamOllamaModel(endpoint: EndpointEntity, history: List<OllamaMessage>, groupId: Long): Boolean {"
content = content.replace(target2, new2)


target3 = """                updateEndpointStatus(endpoint.id, "Working (Last OK: ${java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))})")
            } catch (e: Exception) {
                updateEndpointStatus(endpoint.id, "Error: ${e.message}")
                repository.updateMessage(placeholderMsg.copy(id = insertedId, text = "Error: ${e.message}"))
            }
        }
    }"""
new3 = """                updateEndpointStatus(endpoint.id, "Working (Last OK: ${java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))})")
                return@withContext true
            } catch (e: Exception) {
                updateEndpointStatus(endpoint.id, "Error: ${e.message}")
                repository.updateMessage(placeholderMsg.copy(id = insertedId, text = "Error: ${e.message}"))
                return@withContext false
            }
        }
    }"""
content = content.replace(target3, new3)

target4 = """                updateEndpointStatus(endpoint.id, "Working (Last OK: ${java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))})")
            } catch (e: Exception) {
                updateEndpointStatus(endpoint.id, "Error: ${e.message}")
                var errorMsg = e.message ?: "Unknown Error"
                if (endpoint.url.contains("10.0.2.2")) {
                    errorMsg += "\\n\\n(Fix: 10.0.2.2 only works in the Android Emulator. If you are on a real phone, go to Settings and change the URL to your computer's actual Wi-Fi IP address like 192.168.1.x, and ensure OLLAMA_HOST=0.0.0.0 is set on your PC before starting Ollama.)"
                }
                repository.updateMessage(placeholderMsg.copy(id = insertedId, text = "Network Error: $errorMsg"))
            }
        }
    }"""
new4 = """                updateEndpointStatus(endpoint.id, "Working (Last OK: ${java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))})")
                return@withContext true
            } catch (e: Exception) {
                updateEndpointStatus(endpoint.id, "Error: ${e.message}")
                var errorMsg = e.message ?: "Unknown Error"
                if (endpoint.url.contains("10.0.2.2")) {
                    errorMsg += "\\n\\n(Fix: 10.0.2.2 only works in the Android Emulator. If you are on a real phone, go to Settings and change the URL to your computer's actual Wi-Fi IP address like 192.168.1.x, and ensure OLLAMA_HOST=0.0.0.0 is set on your PC before starting Ollama.)"
                }
                repository.updateMessage(placeholderMsg.copy(id = insertedId, text = "Network Error: $errorMsg"))
                return@withContext false
            }
        }
    }"""
content = content.replace(target4, new4)

old_routing = """                for (endpoint in sortedEndpoints) {
                    try {
                        if (endpoint.type == "OLLAMA") {
                            streamOllamaModel(endpoint, history, groupId)
                        } else {
                            streamOpenRouterModel(endpoint, history, groupId)
                        }
                        success = true
                        break // Stop on first successful response
                    } catch (e: Exception) {
                        lastError = e.message
                        android.util.Log.w("ChatViewModel", "Endpoint ${endpoint.name} failed, falling back... Error: $lastError")
                        // Continue to next endpoint
                    }
                }"""
new_routing = """                for (endpoint in sortedEndpoints) {
                    val isSuccess = if (endpoint.type == "OLLAMA") {
                        streamOllamaModel(endpoint, history, groupId)
                    } else {
                        streamOpenRouterModel(endpoint, history, groupId)
                    }
                    if (isSuccess) {
                        success = true
                        break
                    } else {
                        android.util.Log.w("ChatViewModel", "Endpoint ${endpoint.name} failed, falling back...")
                    }
                }"""
content = content.replace(old_routing, new_routing)

# Also need to return value explicitly if [DONE]
content = content.replace("if (jsonLine == \"[DONE]\") return@let", "if (jsonLine == \"[DONE]\") return@let") # return@let doesn't return the outer function, it just breaks the line iteration. We are good.

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
