import re

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

# Add councilModeFlow to viewModel
target_init = "    val pullMemoryOnStart = settingsRepository.pullMemoryOnStartFlow.stateIn(viewModelScope, SharingStarted.Eagerly, true)"
new_init = target_init + "\n    val councilMode = settingsRepository.councilModeFlow.stateIn(viewModelScope, SharingStarted.Eagerly, false)"
content = content.replace(target_init, new_init)

old_routing = """            val jobs = activeEndpoints.map { endpoint ->
                async {
                    if (endpoint.type == "OLLAMA") {
                        streamOllamaModel(endpoint, history, groupId)
                    } else {
                        streamOpenRouterModel(endpoint, history, groupId)
                    }
                }
            }
            try {
                jobs.awaitAll()
            } catch (e: Exception) {
                _errorMessage.value = "Council Error: ${e.message}"
            }"""

new_routing = """            if (councilMode.value) {
                val jobs = activeEndpoints.map { endpoint ->
                    async {
                        if (endpoint.type == "OLLAMA") {
                            streamOllamaModel(endpoint, history, groupId)
                        } else {
                            streamOpenRouterModel(endpoint, history, groupId)
                        }
                    }
                }
                try {
                    jobs.awaitAll()
                } catch (e: Exception) {
                    _errorMessage.value = "Council Error: ${e.message}"
                }
            } else {
                // Unified Smart Auto-Router Mode
                val sortedEndpoints = activeEndpoints.sortedByDescending { it.isPrimary }
                var success = false
                var lastError: String? = null
                
                for (endpoint in sortedEndpoints) {
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
                }
                
                if (!success) {
                    _errorMessage.value = "All active endpoints failed. Last error: $lastError"
                }
            }"""

content = content.replace(old_routing, new_routing)

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
