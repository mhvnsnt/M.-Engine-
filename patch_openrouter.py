import re

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

old_openrouter = """        withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.openRouterService.generateChatStream(
                    url = endpoint.url,
                    authHeader = "Bearer ${endpoint.apiKey}",
                    request = request
                )"""

new_openrouter = """        withContext(Dispatchers.IO) {
            try {
                var response: okhttp3.ResponseBody? = null
                var attempt = 0
                val maxRetries = 3
                while (attempt < maxRetries) {
                    try {
                        response = RetrofitClient.openRouterService.generateChatStream(
                            url = endpoint.url,
                            authHeader = "Bearer ${endpoint.apiKey}",
                            request = request
                        )
                        break
                    } catch (e: Exception) {
                        attempt++
                        android.util.Log.e("ChatViewModel", "OpenRouter connection attempt $attempt failed for ${endpoint.url}: ${e.message}")
                        if (attempt >= maxRetries) throw e
                        kotlinx.coroutines.delay(1000L * attempt)
                    }
                }
                if (response == null) throw Exception("Failed to connect after $maxRetries attempts")
"""

content = content.replace(old_openrouter, new_openrouter)

old_or_read = """                val reader = BufferedReader(InputStreamReader(response.byteStream()))"""
new_or_read = """                val reader = BufferedReader(InputStreamReader(response!!.byteStream()))"""
content = content.replace(old_or_read, new_or_read)

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
