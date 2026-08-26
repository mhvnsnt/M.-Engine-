package com.example.network

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Streaming
import retrofit2.http.Url

interface OllamaApiService {
    @Streaming
    @POST
    suspend fun generateChatStream(
        @Url url: String,
        @Body request: OllamaChatRequest
    ): retrofit2.Response<ResponseBody>
}
