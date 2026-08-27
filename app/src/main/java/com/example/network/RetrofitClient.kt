package com.example.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory


object RetrofitClient {
    private val sanitizedLogger = object : HttpLoggingInterceptor.Logger {
        override fun log(message: String) {
            var sanitized = message
            // Redact Telegram Bot Tokens in URLs: /bot<token>/
            sanitized = sanitized.replace(Regex("/bot[0-9]+:[a-zA-Z0-9_\\-]+/"), "/bot[REDACTED]/")
            // Redact explicit Bearer tokens in body or headers
            sanitized = sanitized.replace(Regex("Bearer\\s+[a-zA-Z0-9_\\-\\.]+"), "Bearer [REDACTED]")
            // Redact raw GitHub PAT tokens (ghp_...)
            sanitized = sanitized.replace(Regex("ghp_[a-zA-Z0-9]+"), "[REDACTED_PAT]")
            // Redact standard OpenRouter keys (sk-or-v1-...)
            sanitized = sanitized.replace(Regex("sk-or-v1-[a-zA-Z0-9]+"), "[REDACTED_OPENROUTER_KEY]")
            
            android.util.Log.d("OkHttp", sanitized)
        }
    }

    private val loggingInterceptor = HttpLoggingInterceptor(sanitizedLogger).apply {
        level = if (com.example.BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        redactHeader("Authorization")
        redactHeader("Cookie")
        redactHeader("X-Api-Key")
    }

    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: OllamaApiService by lazy {
        Retrofit.Builder()
            .baseUrl("http://localhost/") // Base URL is ignored when @Url is used
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OllamaApiService::class.java)
    }

    val openRouterService: OpenRouterApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://openrouter.ai/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenRouterApiService::class.java)
    }
    val githubService: GitHubApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GitHubApiService::class.java)
    }
    
    val telegramService: TelegramApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.telegram.org/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TelegramApiService::class.java)
    }
    val githubAuthService: GitHubAuthService by lazy {
        Retrofit.Builder()
            .baseUrl("https://github.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GitHubAuthService::class.java)
    }
}


