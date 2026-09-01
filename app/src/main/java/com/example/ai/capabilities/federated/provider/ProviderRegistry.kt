package com.example.ai.capabilities.federated.provider

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


class ProviderRegistry () {

    private val _providers = MutableStateFlow<Map<String, CapabilityProvider>>(emptyMap())
    val providers: StateFlow<Map<String, CapabilityProvider>> = _providers.asStateFlow()

    fun register(provider: CapabilityProvider) {
        val current = _providers.value.toMutableMap()
        current[provider.providerId] = provider
        _providers.value = current
    }

    fun getProvidersByType(type: CapabilityType): List<CapabilityProvider> {
        return _providers.value.values.filter { it.capabilityType == type }
    }
    
    fun getProvider(id: String): CapabilityProvider? {
        return _providers.value[id]
    }
}
