package com.example.network

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenRouterRequest(
    val model: String,
    val messages: List<OllamaMessage>,
    val stream: Boolean = true
)

@JsonClass(generateAdapter = true)
data class OpenRouterResponse(
    val choices: List<OpenRouterChoice>? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterChoice(
    val delta: OpenRouterDelta? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterDelta(
    val content: String? = null
)
