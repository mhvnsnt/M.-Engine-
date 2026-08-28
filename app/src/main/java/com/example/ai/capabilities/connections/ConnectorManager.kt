package com.example.ai.capabilities.connections

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConnectorManager(
    private val providers: Set<ConnectionProvider>
) {
    private val _connectionStates = MutableStateFlow<Map<String, ConnectionStatus>>(emptyMap())
    val connectionStates: StateFlow<Map<String, ConnectionStatus>> = _connectionStates.asStateFlow()

    suspend fun checkAllHealth() {
        val states = mutableMapOf<String, ConnectionStatus>()
        providers.forEach { provider ->
            states[provider.id] = provider.healthCheck()
        }
        _connectionStates.value = states
    }

    fun getProvider(id: String): ConnectionProvider? = providers.find { it.id == id }
    fun getAllProviders(): List<ConnectionProvider> = providers.toList()
    
    fun getProvidersWithCapability(capability: CapabilityType): List<ConnectionProvider> {
        // Need to run discoverCapabilities on them ideally, but since it's suspend we might need to filter manually or rely on pre-fetched.
        // For simplicity right now, we can just return a flow or suspending function
        return emptyList() // Will be implemented as suspending if needed
    }

    suspend fun findProvidersForCapability(capability: CapabilityType): List<ConnectionProvider> {
        return providers.filter { capability in it.discoverCapabilities() }
    }

    suspend fun connectProvider(id: String, activity: Activity? = null): AuthorizationResult {
        val provider = getProvider(id) ?: return AuthorizationResult.Error("Provider not found")
        val result = provider.connect(activity)
        if (result is AuthorizationResult.Success) {
            checkAllHealth()
        }
        return result
    }

    suspend fun verifyProvider(id: String): VerificationResult {
        val provider = getProvider(id) ?: return VerificationResult.Error("Provider not found")
        val result = provider.verify()
        checkAllHealth() // status might update to error if verify fails
        return result
    }

    suspend fun disconnectProvider(id: String) {
        val provider = getProvider(id) ?: return
        provider.disconnect()
        checkAllHealth()
    }
}
