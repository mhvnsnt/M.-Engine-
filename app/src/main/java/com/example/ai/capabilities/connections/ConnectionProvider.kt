package com.example.ai.capabilities.connections

enum class ConnectionStatus {
    CONNECTED,
    DISCONNECTED,
    ERROR,
    UNCONFIGURED,
    PENDING_AUTHORIZATION
}

enum class RealityClassification {
    REAL_AND_CONNECTED,
    REAL_BUT_UNCONFIGURED,
    REAL_BUT_UNVERIFIED,
    BLOCKED_BY_EXTERNAL_DEPENDENCY,
    PARTIAL_REAL_IMPLEMENTATION,
    SIMULATION,
    MOCK,
    STUB
}

enum class CapabilityType {
    GITHUB_API,
    FIREBASE_DEPLOYMENT,
    GOOGLE_CLOUD,
    LLM_INFERENCE,
    CI_CD,
    APP_DISTRIBUTION,
    STORAGE,
    ANDROID_DEVICE
}

sealed class AuthorizationResult {
    data class Success(val token: String? = null, val message: String = "Authorized") : AuthorizationResult()
    data class Error(val message: String) : AuthorizationResult()
    object PendingUserAction : AuthorizationResult()
}

sealed class VerificationResult {
    data class Success(val evidence: String) : VerificationResult()
    data class Error(val message: String) : VerificationResult()
}

interface ConnectionProvider {
    val id: String
    val name: String
    val classification: RealityClassification
    
    suspend fun discoverCapabilities(): Set<CapabilityType>
    
    suspend fun connect(activity: android.app.Activity? = null): AuthorizationResult
    suspend fun disconnect()
    
    suspend fun authenticate(activity: android.app.Activity? = null): AuthorizationResult
    suspend fun verify(): VerificationResult
    
    suspend fun refresh(): AuthorizationResult
    suspend fun revoke()
    suspend fun healthCheck(): ConnectionStatus
}
