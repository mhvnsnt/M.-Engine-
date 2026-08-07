with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

target = """            if (repository.getEndpointCount() == 0) {
                repository.insertEndpoint(EndpointEntity(
                    name = "Local Ollama",
                    url = "http://10.0.2.2:11434/api/chat",
                    apiKey = "",
                    modelName = "gemma:2b",
                    type = "OLLAMA",
                    isActive = true,
                    isPrimary = true
                ))
                repository.insertEndpoint(EndpointEntity(
                    name = "Groq (Llama 3)",
                    url = "https://api.groq.com/openai/v1/chat/completions",
                    apiKey = "",
                    modelName = "llama3-8b-8192",
                    type = "OPENAI",
                    isActive = false,
                    isPrimary = false
                ))
            }"""

replacement = """            if (repository.getEndpointCount() == 0) {
                repository.insertEndpoint(EndpointEntity(
                    name = "Local Ollama (Gemma)",
                    url = "http://10.0.2.2:11434/api/chat",
                    apiKey = "",
                    modelName = "gemma:2b",
                    type = "OLLAMA",
                    isActive = true,
                    isPrimary = true
                ))
                repository.insertEndpoint(EndpointEntity(
                    name = "Local Ollama (Llama 3 Abliterated)",
                    url = "http://10.0.2.2:11434/api/chat",
                    apiKey = "",
                    modelName = "llama3:8b-instruct-fp16",
                    type = "OLLAMA",
                    isActive = true,
                    isPrimary = false
                ))
                repository.insertEndpoint(EndpointEntity(
                    name = "Groq (Llama 3 8B)",
                    url = "https://api.groq.com/openai/v1/chat/completions",
                    apiKey = "",
                    modelName = "llama3-8b-8192",
                    type = "OPENAI",
                    isActive = true,
                    isPrimary = false
                ))
                repository.insertEndpoint(EndpointEntity(
                    name = "Groq (Mixtral 8x7B)",
                    url = "https://api.groq.com/openai/v1/chat/completions",
                    apiKey = "",
                    modelName = "mixtral-8x7b-32768",
                    type = "OPENAI",
                    isActive = true,
                    isPrimary = false
                ))
                repository.insertEndpoint(EndpointEntity(
                    name = "OpenRouter (Dolphin Llama 3 8B Uncensored)",
                    url = "https://openrouter.ai/api/v1/chat/completions",
                    apiKey = "",
                    modelName = "cognitivecomputations/dolphin-llama-3-8b",
                    type = "OPENAI",
                    isActive = true,
                    isPrimary = false
                ))
                repository.insertEndpoint(EndpointEntity(
                    name = "OpenRouter (Hermes 3 8B Uncensored)",
                    url = "https://openrouter.ai/api/v1/chat/completions",
                    apiKey = "",
                    modelName = "nousresearch/hermes-3-llama-3.1-8b",
                    type = "OPENAI",
                    isActive = true,
                    isPrimary = false
                ))
                repository.insertEndpoint(EndpointEntity(
                    name = "OpenRouter (Llama 3.1 8B Free)",
                    url = "https://openrouter.ai/api/v1/chat/completions",
                    apiKey = "",
                    modelName = "meta-llama/llama-3.1-8b-instruct:free",
                    type = "OPENAI",
                    isActive = true,
                    isPrimary = false
                ))
            }"""

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
        f.write(content)
    print("Replaced successfully")
else:
    print("Target not found")
