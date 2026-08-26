import re

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

# Fix councilMode not being defined
target1 = "    val pullMemoryOnStart: StateFlow<Boolean> = settingsRepository.pullMemoryOnStartFlow.stateIn(viewModelScope, SharingStarted.Lazily, false)"
new1 = target1 + "\n    val councilMode: StateFlow<Boolean> = settingsRepository.councilModeFlow.stateIn(viewModelScope, SharingStarted.Lazily, false)"
content = content.replace(target1, new1)

# Fix updateEndpointStatus
target2 = """                updateEndpointStatus(endpoint.id, "Working (Last OK: ${java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))})")"""
new2 = """                _endpointStatuses.value = _endpointStatuses.value.toMutableMap().apply { put(endpoint.id, "Working (Last OK: ${java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))})") }"""
content = content.replace(target2, new2)

target3 = """                updateEndpointStatus(endpoint.id, "Error: ${e.message}")"""
new3 = """                _endpointStatuses.value = _endpointStatuses.value.toMutableMap().apply { put(endpoint.id, "Error: ${e.message}") }"""
content = content.replace(target3, new3)

# Fix missing return statement
target4 = """            } catch (e: Exception) {
                _endpointStatuses.value = _endpointStatuses.value.toMutableMap().apply { put(endpoint.id, "Error: ${e.message}") }
                repository.updateMessage(placeholderMsg.copy(id = insertedId, text = "Error: ${e.message}"))
                return@withContext false
            }
        }
    }"""
new4 = """            } catch (e: Exception) {
                _endpointStatuses.value = _endpointStatuses.value.toMutableMap().apply { put(endpoint.id, "Error: ${e.message}") }
                repository.updateMessage(placeholderMsg.copy(id = insertedId, text = "Error: ${e.message}"))
                return@withContext false
            }
        }
        return false // Fallback
    }"""
content = content.replace(target4, new4)

target5 = """            } catch (e: Exception) {
                _endpointStatuses.value = _endpointStatuses.value.toMutableMap().apply { put(endpoint.id, "Error: ${e.message}") }
                var errorMsg = e.message ?: "Unknown Error"
                if (endpoint.url.contains("10.0.2.2")) {
                    errorMsg += "\\n\\n(Fix: 10.0.2.2 only works in the Android Emulator. If you are on a real phone, go to Settings and change the URL to your computer's actual Wi-Fi IP address like 192.168.1.x, and ensure OLLAMA_HOST=0.0.0.0 is set on your PC before starting Ollama.)"
                }
                repository.updateMessage(placeholderMsg.copy(id = insertedId, text = "Network Error: $errorMsg"))
                return@withContext false
            }
        }
    }"""
new5 = """            } catch (e: Exception) {
                _endpointStatuses.value = _endpointStatuses.value.toMutableMap().apply { put(endpoint.id, "Error: ${e.message}") }
                var errorMsg = e.message ?: "Unknown Error"
                if (endpoint.url.contains("10.0.2.2")) {
                    errorMsg += "\\n\\n(Fix: 10.0.2.2 only works in the Android Emulator. If you are on a real phone, go to Settings and change the URL to your computer's actual Wi-Fi IP address like 192.168.1.x, and ensure OLLAMA_HOST=0.0.0.0 is set on your PC before starting Ollama.)"
                }
                repository.updateMessage(placeholderMsg.copy(id = insertedId, text = "Network Error: $errorMsg"))
                return@withContext false
            }
        }
        return false // Fallback
    }"""
content = content.replace(target5, new5)

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
