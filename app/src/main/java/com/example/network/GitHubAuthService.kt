package com.example.network

import retrofit2.http.POST
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Field
import retrofit2.http.Headers

interface GitHubAuthService {
    @Headers("Accept: application/json")
    @FormUrlEncoded
    @POST("login/device/code")
    suspend fun getDeviceCode(
        @Field("client_id") clientId: String,
        @Field("scope") scope: String = "repo"
    ): DeviceCodeResponse

    @Headers("Accept: application/json")
    @FormUrlEncoded
    @POST("login/oauth/access_token")
    suspend fun getAccessToken(
        @Field("client_id") clientId: String,
        @Field("device_code") deviceCode: String,
        @Field("grant_type") grantType: String = "urn:ietf:params:oauth:grant-type:device_code"
    ): AccessTokenResponse
}

data class DeviceCodeResponse(
    val device_code: String,
    val user_code: String,
    val verification_uri: String,
    val expires_in: Int,
    val interval: Int
)

data class AccessTokenResponse(
    val access_token: String?,
    val error: String?,
    val error_description: String?
)
