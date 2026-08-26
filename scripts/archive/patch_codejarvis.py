import re

with open("app/src/main/java/com/example/ai/CodeJarvis.kt", "r") as f:
    content = f.read()

old_call_model = """    private suspend fun callModel(endpoint: EndpointEntity, systemPrompt: String, userPrompt: String): String {"""

new_call_model = """    private suspend fun callModel(endpoint: EndpointEntity, systemPrompt: String, userPrompt: String): String {
        return if (endpoint.url.contains("openrouter")) {
            val req = OpenRouterRequest(
                model = endpoint.modelName,
                messages = listOf(
                    OpenRouterMessage(role = "system", content = listOf(OpenRouterContentPart(type = "text", text = systemPrompt))),
                    OpenRouterMessage(role = "user", content = listOf(OpenRouterContentPart(type = "text", text = userPrompt)))
                ),
                stream = false
            )
            val response = RetrofitClient.openRouterService.generateChatStream(endpoint.url, "Bearer ${endpoint.apiKey}", request = req)
            val reader = java.io.BufferedReader(java.io.InputStreamReader(response.byteStream()))
            var completeResponse = ""
            var line: String?
            val adapter = com.squareup.moshi.Moshi.Builder().add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build().adapter(com.example.network.OpenRouterResponse::class.java)
            while (reader.readLine().also { line = it } != null) {
                line?.let { jsonLine ->
                    if (jsonLine.startsWith("data: ")) {
                        val data = jsonLine.substring(6)
                        if (data != "[DONE]") {
                            try {
                                val chunk = adapter.fromJson(data)
                                chunk?.choices?.firstOrNull()?.delta?.content?.let { completeResponse += it }
                            } catch (e: Exception) {}
                        }
                    } else if (jsonLine.startsWith("{")) {
                        try {
                            val resp = adapter.fromJson(jsonLine)
                            resp?.choices?.firstOrNull()?.message?.content?.let { completeResponse += it }
                        } catch (e: Exception) {}
                    }
                }
            }
            completeResponse
        } else {
            val req = OllamaChatRequest(
                model = endpoint.modelName,
                messages = listOf(
                    OllamaMessage(role = "system", content = systemPrompt),
                    OllamaMessage(role = "user", content = userPrompt)
                ),
                stream = false
            )
            val response = RetrofitClient.service.generateChatStream(endpoint.url, req)
            val reader = java.io.BufferedReader(java.io.InputStreamReader(response.byteStream()))
            var completeResponse = ""
            var line: String?
            val adapter = com.squareup.moshi.Moshi.Builder().add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build().adapter(com.example.network.OllamaChatResponse::class.java)
            while (reader.readLine().also { line = it } != null) {
                line?.let { jsonLine ->
                    try {
                        val chunk = adapter.fromJson(jsonLine)
                        chunk?.message?.content?.let { completeResponse += it }
                    } catch (e: Exception) {}
                }
            }
            completeResponse
        }
    }"""

content = re.sub(r'    private suspend fun callModel.*?\}', new_call_model, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/ai/CodeJarvis.kt", "w") as f:
    f.write(content)
