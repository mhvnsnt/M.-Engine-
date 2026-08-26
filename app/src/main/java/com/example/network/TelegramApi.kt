package com.example.network

import com.squareup.moshi.JsonClass
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class TelegramUpdateResponse(
    val ok: Boolean,
    val result: List<TelegramUpdate>?
)

@JsonClass(generateAdapter = true)
data class TelegramUpdate(
    val update_id: Long,
    val message: TelegramMessage?
)

@JsonClass(generateAdapter = true)
data class TelegramMessage(
    val message_id: Long,
    val from: TelegramUser?,
    val chat: TelegramChat,
    val text: String?
)

@JsonClass(generateAdapter = true)
data class TelegramUser(
    val id: Long,
    val first_name: String,
    val username: String?
)

@JsonClass(generateAdapter = true)
data class TelegramChat(
    val id: Long,
    val type: String
)

@JsonClass(generateAdapter = true)
data class TelegramMessageRequest(
    val chat_id: Long,
    val text: String
)

@JsonClass(generateAdapter = true)
data class TelegramMessageResponse(
    val ok: Boolean
)

interface TelegramApiService {
    @GET("bot{token}/getUpdates")
    suspend fun getUpdates(
        @Path("token") token: String,
        @Query("offset") offset: Long?,
        @Query("timeout") timeout: Int = 30
    ): TelegramUpdateResponse

    @POST("bot{token}/sendMessage")
    suspend fun sendMessage(
        @Path("token") token: String,
        @Body request: TelegramMessageRequest
    ): TelegramMessageResponse

    @Multipart
    @POST("bot{token}/sendDocument")
    suspend fun sendDocument(
        @Path("token") token: String,
        @Part("chat_id") chatId: RequestBody,
        @Part document: MultipartBody.Part,
        @Part("caption") caption: RequestBody? = null
    ): TelegramMessageResponse
}
