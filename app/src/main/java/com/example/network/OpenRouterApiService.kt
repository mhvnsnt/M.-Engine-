package com.example.network

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming
import retrofit2.http.Url

interface OpenRouterApiService {
    @Streaming
    @POST
    suspend fun generateChatStream(
        @Url url: String,
        @Header("Authorization") authHeader: String,
        @Header("HTTP-Referer") referer: String = "https://aistudio.google.com",
        @Header("X-Title") title: String = "M. Engine",
        @Body request: OpenRouterRequest
    ): ResponseBody
}
