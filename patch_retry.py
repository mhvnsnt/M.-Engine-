import re

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

# Make OpenRouter primary
content = content.replace('''                repository.insertEndpoint(EndpointEntity(
                    name = "Local Ollama (Gemma)",
                    url = "http://10.0.2.2:11434/api/chat",
                    apiKey = "",
                    modelName = "gemma:2b",
                    type = "OLLAMA",
                    isActive = true,
                    isPrimary = true
                ))''', '''                repository.insertEndpoint(EndpointEntity(
                    name = "Local Ollama (Gemma)",
                    url = "http://10.0.2.2:11434/api/chat",
                    apiKey = "",
                    modelName = "gemma:2b",
                    type = "OLLAMA",
                    isActive = true,
                    isPrimary = false
                ))''')

content = content.replace('''                repository.insertEndpoint(EndpointEntity(
                    name = "OpenRouter (Llama 3.1 8B Free)",
                    url = "https://openrouter.ai/api/v1/chat/completions",
                    apiKey = "",
                    modelName = "meta-llama/llama-3.1-8b-instruct:free",
                    type = "OPENAI",
                    isActive = true,
                    isPrimary = false
                ))''', '''                repository.insertEndpoint(EndpointEntity(
                    name = "Google Gemini (Gemini 2.5 Flash Free)",
                    url = "https://openrouter.ai/api/v1/chat/completions",
                    apiKey = "",
                    modelName = "google/gemini-2.5-flash:free",
                    type = "OPENAI",
                    isActive = true,
                    isPrimary = true
                ))
                repository.insertEndpoint(EndpointEntity(
                    name = "OpenRouter (Llama 3.1 8B Free)",
                    url = "https://openrouter.ai/api/v1/chat/completions",
                    apiKey = "",
                    modelName = "meta-llama/llama-3.1-8b-instruct:free",
                    type = "OPENAI",
                    isActive = true,
                    isPrimary = false
                ))''')


with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
