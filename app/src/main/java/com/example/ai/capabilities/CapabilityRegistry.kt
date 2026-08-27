package com.example.ai.capabilities

import com.example.ai.PermissionLevel

enum class CapabilityType {
    MODEL, LOCAL_INFERENCE, MEMORY, VOICE, VISION, 
    TOOLS, GITHUB, REMOTE_AGENT, BUILD, TEST, WEB
}

enum class CapabilityStatus { ONLINE, OFFLINE, UNAVAILABLE }

interface CapabilityProvider {
    val name: String
    val type: CapabilityType
    val isLocal: Boolean
    val status: CapabilityStatus
    val permissionLevel: PermissionLevel
    val supportedOperations: List<String>
    val networkRequired: Boolean
}

interface CapabilityRegistry {
    fun register(provider: CapabilityProvider)
    fun getProviders(type: CapabilityType): List<CapabilityProvider>
    fun isAvailable(type: CapabilityType): Boolean
}

class CapabilityRegistryImpl : CapabilityRegistry {
    private val providers = mutableListOf<CapabilityProvider>()
    
    override fun register(provider: CapabilityProvider) {
        providers.add(provider)
    }

    override fun getProviders(type: CapabilityType): List<CapabilityProvider> {
        return providers.filter { it.type == type }
    }

    override fun isAvailable(type: CapabilityType): Boolean {
        return providers.any { it.type == type && (it.status == CapabilityStatus.ONLINE || it.status == CapabilityStatus.OFFLINE) }
    }
}
