import re

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

target = """        viewModelScope.launch {
            if (repository.getEndpointCount() == 0) {"""
new_target = """        viewModelScope.launch {
            val allEndpoints = repository.getAllEndpoints()
            val hasGemini = allEndpoints.any { it.modelName == "google/gemini-2.5-flash:free" }
            if (!hasGemini) {
                repository.insertEndpoint(com.example.data.EndpointEntity(
                    name = "Google Gemini (Gemini 2.5 Flash Free)",
                    url = "https://openrouter.ai/api/v1/chat/completions",
                    apiKey = "",
                    modelName = "google/gemini-2.5-flash:free",
                    type = "OPENAI",
                    isActive = true,
                    isPrimary = true
                ))
            }
            if (repository.getEndpointCount() <= 1) {"""

content = content.replace(target, new_target)

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
