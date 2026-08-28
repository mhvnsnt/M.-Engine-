package com.example.ai.capabilities.connections

import android.app.Activity
import com.example.data.SettingsRepository
import kotlinx.coroutines.flow.firstOrNull

class OpenRouterConnectionProvider(
    private val settingsRepository: SettingsRepository
) : ConnectionProvider {
    override val id = "openrouter"
    override val name = "OpenRouter (Delegated Auth)"
    override val classification = RealityClassification.BLOCKED_BY_EXTERNAL_DEPENDENCY // Missing secure PKCE backend

    override suspend fun discoverCapabilities(): Set<CapabilityType> = setOf(CapabilityType.LLM_INFERENCE)

    override suspend fun healthCheck(): ConnectionStatus {
        val key = settingsRepository.openRouterKeyFlow.firstOrNull()
        return if (!key.isNullOrEmpty()) {
            ConnectionStatus.CONNECTED
        } else {
            ConnectionStatus.UNCONFIGURED
        }
    }

    override suspend fun connect(activity: Activity?): AuthorizationResult {
        return authenticate(activity)
    }

    override suspend fun disconnect() {
        revoke()
    }

    override suspend fun authenticate(activity: Activity?): AuthorizationResult {
        // OpenRouter requires a secure deep link PKCE flow backend which we lack right now.
        return AuthorizationResult.PendingUserAction
    }

    override suspend fun verify(): VerificationResult {
        val key = settingsRepository.openRouterKeyFlow.firstOrNull()
        if (key.isNullOrEmpty()) return VerificationResult.Error("No token available")
        return VerificationResult.Success("OpenRouter capability verified locally")
    }

    override suspend fun refresh(): AuthorizationResult {
        return authenticate(null)
    }

    override suspend fun revoke() {
        settingsRepository.updateOpenRouterKey("")
    }
}
