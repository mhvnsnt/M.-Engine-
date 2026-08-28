package com.example.ai.capabilities

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

enum class ProviderErrorKind {
    RATE_LIMITED,
    QUOTA_EXHAUSTED,
    AUTH_FAILED,
    MODEL_NOT_FOUND,
    CONTEXT_LENGTH_EXCEEDED,
    SERVICE_OVERLOADED,
    NETWORK_ERROR,
    MALFORMED_RESPONSE,
    UNKNOWN
}

data class ProviderErrorInfo(
    val kind: ProviderErrorKind,
    val httpCode: Int? = null,
    val rawMessage: String,
    val recommendedCooldownMs: Long,
    val isRetryable: Boolean
)

object ProviderErrorClassifier {

    fun classify(
        throwable: Throwable? = null,
        httpCode: Int? = null,
        responseBody: String? = null
    ): ProviderErrorInfo {
        val message = (responseBody ?: throwable?.message ?: "Unknown error").lowercase()
        val code = httpCode ?: extractHttpCode(throwable)

        return when {
            // Quota / Credit limit exceeded (HTTP 402 or 429 with specific quota messages)
            code == 402 || 
            message.contains("insufficient_quota") ||
            message.contains("quota exceeded") ||
            message.contains("out of credits") ||
            message.contains("exceeded your current quota") ||
            message.contains("credit balance is too low") -> {
                ProviderErrorInfo(
                    kind = ProviderErrorKind.QUOTA_EXHAUSTED,
                    httpCode = code ?: 429,
                    rawMessage = message,
                    recommendedCooldownMs = 15 * 60 * 1000L, // 15 minutes cooldown
                    isRetryable = false
                )
            }

            // Rate limit / TPM / RPM throttling (HTTP 429)
            code == 429 || message.contains("rate limit") || message.contains("too many requests") -> {
                ProviderErrorInfo(
                    kind = ProviderErrorKind.RATE_LIMITED,
                    httpCode = code ?: 429,
                    rawMessage = message,
                    recommendedCooldownMs = 60 * 1000L, // 60 seconds cooldown
                    isRetryable = true
                )
            }

            // Authentication & Authorization failures
            code == 401 || code == 403 || message.contains("unauthorized") || 
            message.contains("invalid api key") || message.contains("permission denied") ||
            message.contains("forbidden") -> {
                ProviderErrorInfo(
                    kind = ProviderErrorKind.AUTH_FAILED,
                    httpCode = code ?: 401,
                    rawMessage = message,
                    recommendedCooldownMs = 30 * 60 * 1000L, // 30 minutes cooldown
                    isRetryable = false
                )
            }

            // Model not found or deprecated
            code == 404 || message.contains("model not found") || message.contains("model is deprecated") ||
            message.contains("does not exist") || message.contains("unknown model") -> {
                ProviderErrorInfo(
                    kind = ProviderErrorKind.MODEL_NOT_FOUND,
                    httpCode = code ?: 404,
                    rawMessage = message,
                    recommendedCooldownMs = 60 * 60 * 1000L, // 1 hour cooldown
                    isRetryable = false
                )
            }

            // Context window length exceeded
            message.contains("context_length_exceeded") || message.contains("maximum context length") ||
            message.contains("prompt is too long") || message.contains("tokens exceeds") -> {
                ProviderErrorInfo(
                    kind = ProviderErrorKind.CONTEXT_LENGTH_EXCEEDED,
                    httpCode = code ?: 400,
                    rawMessage = message,
                    recommendedCooldownMs = 0L, // No cooldown needed, request-specific
                    isRetryable = false
                )
            }

            // Service overloaded or transient server errors (500, 502, 503, 504)
            code in listOf(500, 502, 503, 504) || message.contains("overloaded") || 
            message.contains("service unavailable") || message.contains("bad gateway") ||
            message.contains("server error") -> {
                ProviderErrorInfo(
                    kind = ProviderErrorKind.SERVICE_OVERLOADED,
                    httpCode = code ?: 503,
                    rawMessage = message,
                    recommendedCooldownMs = 10 * 1000L, // 10 seconds cooldown
                    isRetryable = true
                )
            }

            // Network / Connection issues
            throwable is SocketTimeoutException || message.contains("timeout") -> {
                ProviderErrorInfo(
                    kind = ProviderErrorKind.NETWORK_ERROR,
                    httpCode = null,
                    rawMessage = "Socket timeout: $message",
                    recommendedCooldownMs = 15 * 1000L,
                    isRetryable = true
                )
            }

            throwable is ConnectException || throwable is UnknownHostException || throwable is IOException ||
            message.contains("failed to connect") || message.contains("connection refused") -> {
                ProviderErrorInfo(
                    kind = ProviderErrorKind.NETWORK_ERROR,
                    httpCode = null,
                    rawMessage = "Connection failed: $message",
                    recommendedCooldownMs = 30 * 1000L,
                    isRetryable = true
                )
            }

            // Malformed response
            message.contains("json") && (message.contains("malformed") || message.contains("unexpected end of json")) -> {
                ProviderErrorInfo(
                    kind = ProviderErrorKind.MALFORMED_RESPONSE,
                    httpCode = null,
                    rawMessage = message,
                    recommendedCooldownMs = 5 * 1000L,
                    isRetryable = true
                )
            }

            else -> {
                ProviderErrorInfo(
                    kind = ProviderErrorKind.UNKNOWN,
                    httpCode = code,
                    rawMessage = message,
                    recommendedCooldownMs = 10 * 1000L,
                    isRetryable = true
                )
            }
        }
    }

    private fun extractHttpCode(throwable: Throwable?): Int? {
        if (throwable == null) return null
        val msg = throwable.message ?: return null
        val match = Regex("""HTTP\s+(\d{3})""").find(msg) ?: Regex("""code\s*[:=]\s*(\d{3})""").find(msg)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }
}
