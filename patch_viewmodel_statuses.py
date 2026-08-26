import re

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

# Add endpoint statuses
target1 = "    private val _deviceFlowState = MutableStateFlow(DeviceFlowState())"
replacement1 = """    private val _deviceFlowState = MutableStateFlow(DeviceFlowState())
    
    private val _endpointStatuses = MutableStateFlow<Map<Int, String>>(emptyMap())
    val endpointStatuses: StateFlow<Map<Int, String>> = _endpointStatuses.asStateFlow()
    
    fun updateEndpointStatus(id: Int, status: String) {
        _endpointStatuses.value = _endpointStatuses.value.toMutableMap().apply { put(id, status) }
    }"""
content = content.replace(target1, replacement1)

# Inside streamOpenRouterModel, update status
old_or = """            } catch (e: Exception) {
                repository.updateMessage(placeholderMsg.copy(id = insertedId, text = "Error: ${e.message}"))
            }"""
new_or = """                updateEndpointStatus(endpoint.id, "Working (Last OK: ${java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))})")
            } catch (e: Exception) {
                updateEndpointStatus(endpoint.id, "Error: ${e.message}")
                repository.updateMessage(placeholderMsg.copy(id = insertedId, text = "Error: ${e.message}"))
            }"""
content = content.replace(old_or, new_or)

old_ollama = """            } catch (e: Exception) {
                var errorMsg = e.message ?: "Unknown Error"
                if (endpoint.url.contains("10.0.2.2")) {"""
new_ollama = """                updateEndpointStatus(endpoint.id, "Working (Last OK: ${java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))})")
            } catch (e: Exception) {
                updateEndpointStatus(endpoint.id, "Error: ${e.message}")
                var errorMsg = e.message ?: "Unknown Error"
                if (endpoint.url.contains("10.0.2.2")) {"""
content = content.replace(old_ollama, new_ollama)

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
