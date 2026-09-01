package com.example.ai.capabilities.ecology

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class EndpointEnvironment(val displayName: String, val defaultUrl: String) {
    LOCAL_EMULATOR("Local Emulator", "http://10.0.2.2:8080/"),
    LOCAL_NETWORK("Local LAN / Tunnel", "http://192.168.1.100:8080/"),
    STAGING("Cloud Staging", "https://staging-control-plane.mengine.internal/"),
    PRODUCTION("Cloud Production", "https://control-plane.mengine.internal/"),
    CUSTOM("Custom Endpoint", "http://10.0.2.2:8080/")
}

enum class TransportSecurityState {
    PLAINTEXT_HTTP,
    TLS_SECURE,
    UNKNOWN
}

data class ConnectionDiagnostic(
    val environment: EndpointEnvironment,
    val endpointUrl: String,
    val transportSecurity: TransportSecurityState,
    val lastSuccessfulHeartbeat: Long? = null,
    val lastFailure: String? = null,
    val lastFailureTimestamp: Long? = null,
    val activeGovernorState: RemoteGovernorState = RemoteGovernorState.OFFLINE
)

object RemoteEndpointConfiguration {
    private val _selectedEnvironment = MutableStateFlow(EndpointEnvironment.LOCAL_EMULATOR)
    val selectedEnvironment: StateFlow<EndpointEnvironment> = _selectedEnvironment.asStateFlow()

    private val _customUrl = MutableStateFlow("http://10.0.2.2:8080/")
    val customUrl: StateFlow<String> = _customUrl.asStateFlow()

    fun getActiveUrl(): String {
        return when (_selectedEnvironment.value) {
            EndpointEnvironment.CUSTOM -> _customUrl.value
            else -> _selectedEnvironment.value.defaultUrl
        }
    }

    fun setEnvironment(env: EndpointEnvironment) {
        _selectedEnvironment.value = env
    }

    fun setCustomUrl(url: String) {
        var cleanUrl = url.trim()
        if (!cleanUrl.endsWith("/")) {
            cleanUrl = "$cleanUrl/"
        }
        _customUrl.value = cleanUrl
        _selectedEnvironment.value = EndpointEnvironment.CUSTOM
    }

    fun getTransportSecurity(url: String): TransportSecurityState {
        return when {
            url.startsWith("https://", ignoreCase = true) -> TransportSecurityState.TLS_SECURE
            url.startsWith("http://", ignoreCase = true) -> TransportSecurityState.PLAINTEXT_HTTP
            else -> TransportSecurityState.UNKNOWN
        }
    }
}
