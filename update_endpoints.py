with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

target = """            val hasGemini = allEndpoints.any { it.modelName == "google/gemini-2.5-flash:free" }
            if (!hasGemini) {
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

replacement = """            val hasPollinations = allEndpoints.any { it.modelName == "llama" && it.url.contains("pollinations") }
            if (!hasPollinations) {
                // Ensure no other endpoints are primary
                allEndpoints.forEach { 
                    if (it.isPrimary) repository.updateEndpoint(it.copy(isPrimary = false))
                }
                repository.insertEndpoint(com.example.data.EndpointEntity(
                    name = "Pollinations (Free Open Source Llama 3)",
                    url = "https://text.pollinations.ai/openai/chat/completions",
                    apiKey = "", // No API key required!
                    modelName = "llama",
                    type = "OPENAI",
                    isActive = true,
                    isPrimary = true
                ))
            }"""

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
        f.write(content)
    print("Replaced successfully")
else:
    print("Target not found")
