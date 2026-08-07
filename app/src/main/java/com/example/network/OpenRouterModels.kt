package com.example.network

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenRouterRequest(
    val model: String,
    val messages: List<OpenRouterMessage>,
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


@JsonClass(generateAdapter = true)
data class OpenRouterMessage(
    val role: String,
    val content: Any // Can be String or List<OpenRouterContentPart>
)

@JsonClass(generateAdapter = true)
data class OpenRouterContentPart(
    val type: String,
    val text: String? = null,
    val image_url: OpenRouterImageUrl? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterImageUrl(
    val url: String
)