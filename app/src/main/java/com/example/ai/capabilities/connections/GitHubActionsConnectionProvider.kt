package com.example.ai.capabilities.connections

import android.app.Activity
import com.example.data.SettingsRepository
import kotlinx.coroutines.flow.firstOrNull

class GitHubActionsConnectionProvider(
    private val settingsRepository: SettingsRepository
) : ConnectionProvider {
    override val id = "github_actions"
    override val name = "GitHub Actions (OIDC/Workload Identity)"
    override val classification = RealityClassification.REAL_BUT_UNVERIFIED // OIDC workflow is configured, but not physically verified yet

    override suspend fun discoverCapabilities(): Set<CapabilityType> = setOf(CapabilityType.CI_CD)

    override suspend fun healthCheck(): ConnectionStatus {
        val pat = settingsRepository.githubPatFlow.firstOrNull()
        return if (!pat.isNullOrEmpty()) {
            ConnectionStatus.CONNECTED
        } else {
            ConnectionStatus.UNCONFIGURED
        }
    }

    override suspend fun connect(activity: Activity?): AuthorizationResult {
        return AuthorizationResult.Error("Authorization is managed by the main GitHub App connection.")
    }

    override suspend fun disconnect() {
        revoke()
    }

    override suspend fun authenticate(activity: Activity?): AuthorizationResult {
        return connect(activity)
    }

    override suspend fun verify(): VerificationResult {
        val pat = settingsRepository.githubPatFlow.firstOrNull()
        if (pat.isNullOrEmpty()) return VerificationResult.Error("No GitHub token available")
        return VerificationResult.Success("GitHub Actions capability inferred via main GitHub connection")
    }

    override suspend fun refresh(): AuthorizationResult {
        return connect(null)
    }

    override suspend fun revoke() {
        // No-op, managed by GitHub connection
    }
}
