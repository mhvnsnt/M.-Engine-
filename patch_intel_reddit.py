with open('app/src/main/java/com/example/data/LocalIntelligenceRepository.kt', 'r') as f:
    content = f.read()

target = """    suspend fun getLocalNews(region: String): String = withContext(Dispatchers.IO) {
        // Placeholder for local news RSS
        "Local feeds for $region are quiet."
    }"""
replacement = """    suspend fun getLocalNews(region: String): String = withContext(Dispatchers.IO) {
        try {
            val city = region.substringBefore("_").lowercase().replace(" ", "")
            val url = "https://www.reddit.com/r/$city/top/.rss?t=week"
            val request = Request.Builder().url(url)
                .header("User-Agent", "M.Engine/1.0 by MarquisWhitacre")
                .build()
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val xml = response.body?.string() ?: return@withContext "No news found."
                parseRss(xml, limit = 5)
            } else {
                "Could not reach Reddit for $city."
            }
        } catch (e: Exception) {
            "Error fetching local news: ${e.message}"
        }
    }"""
content = content.replace(target, replacement)
with open('app/src/main/java/com/example/data/LocalIntelligenceRepository.kt', 'w') as f:
    f.write(content)
