package com.example.data

import android.content.Context
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

class LocalIntelligenceRepository(private val context: Context) {
    private val client = OkHttpClient()

    // Fetches Craigslist Events for a region (e.g. "nashville")
    suspend fun getCraigslistEvents(region: String): String = withContext(Dispatchers.IO) {
        try {
            val city = region.substringBefore("_").lowercase().replace(" ", "") 
            val url = "https://$city.craigslist.org/search/eve?format=rss"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val xml = response.body?.string() ?: return@withContext "No events found."
                parseRss(xml, limit = 5)
            } else {
                "Could not reach Craigslist for $city."
            }
        } catch (e: Exception) {
            "Error fetching local events: ${e.message}"
        }
    }
    
    private fun parseRss(xml: String, limit: Int): String {
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(xml))
            var eventType = parser.eventType
            val events = mutableListOf<String>()
            var currentTitle = ""
            
            while (eventType != XmlPullParser.END_DOCUMENT && events.size < limit) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (parser.name == "title") {
                            parser.next()
                            if (parser.text != null && parser.text.trim().isNotEmpty()) {
                                currentTitle = parser.text.trim()
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "item" || parser.name == "entry") {
                            if (currentTitle.isNotBlank()) {
                                events.add("- $currentTitle")
                                currentTitle = "" // reset
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
            return if (events.isNotEmpty()) events.joinToString("\n") else "No events listed."
        } catch (e: Exception) {
            return "Error parsing events."
        }
    }
    
    // Future expansion points for actual network calls to these APIs
    suspend fun getLocalEvents(region: String): String = withContext(Dispatchers.IO) {
        getCraigslistEvents(region)
    }
    
    suspend fun getLocalNews(region: String): String = withContext(Dispatchers.IO) {
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
    }
    
    suspend fun getSceneContext(region: String): String = withContext(Dispatchers.IO) {
        val events = getLocalEvents(region)
        val news = getLocalNews(region)
        "Local Scene Context ($region):\n- Events: \n$events\n- News: $news"
    }
}
