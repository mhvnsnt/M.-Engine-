import re

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

target = """            if (!hasGemini) {
                repository.insertEndpoint(com.example.data.EndpointEntity(
                    name = "Google Gemini (Gemini 2.5 Flash Free)",
                    url = "https://openrouter.ai/api/v1/chat/completions",
                    apiKey = "",
                    modelName = "google/gemini-2.5-flash:free",
                    type = "OPENAI",
                    isActive = true,
                    isPrimary = true
                ))
            }"""

new_target = """            if (!hasGemini) {
                // Ensure no other endpoints are primary
                allEndpoints.forEach { 
                    if (it.isPrimary) repository.updateEndpoint(it.copy(isPrimary = false))
                }
                repository.insertEndpoint(com.example.data.EndpointEntity(
                    name = "Google Gemini (Gemini 2.5 Flash Free)",
                    url = "https://openrouter.ai/api/v1/chat/completions",
                    apiKey = "",
                    modelName = "google/gemini-2.5-flash:free",
                    type = "OPENAI",
                    isActive = true,
                    isPrimary = true
                ))
            }"""

content = content.replace(target, new_target)

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
