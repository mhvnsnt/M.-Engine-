package com.example.network

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OllamaChatRequest(
    val model: String,
    val messages: List<OllamaMessage>,
    val stream: Boolean = true
)

@JsonClass(generateAdapter = true)
data class OllamaMessage(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class OllamaChatResponse(
    val model: String? = null,
    val created_at: String? = null,
    val message: OllamaMessage? = null,
    val done: Boolean = false
)
