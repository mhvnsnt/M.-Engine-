package com.example.ai.capabilities.connections

import android.app.Activity
import android.util.Log
import com.example.data.SettingsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await

class GitHubConnectionProvider(
    private val settingsRepository: SettingsRepository
) : ConnectionProvider {
    override val id = "github_app"
    override val name = "GitHub App Integration"
    override val classification = RealityClassification.REAL_BUT_UNCONFIGURED

    override suspend fun discoverCapabilities(): Set<CapabilityType> = setOf(CapabilityType.GITHUB_API)

    override suspend fun healthCheck(): ConnectionStatus {
        val pat = settingsRepository.githubPatFlow.firstOrNull()
        return if (!pat.isNullOrEmpty()) {
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
        if (activity == null) return AuthorizationResult.Error("Activity context required for GitHub App/OAuth delegated flow")
        
        return try {
            val provider = OAuthProvider.newBuilder("github.com")
            // In a real GitHub App context, this would be a web flow to install the App
            // For now, we simulate the delegated OAuth behavior via Firebase
            val auth = FirebaseAuth.getInstance()
            val authResult = auth.startActivityForSignInWithProvider(activity, provider.build()).await()
            val credential = authResult.credential as? com.google.firebase.auth.OAuthCredential
            val token = credential?.accessToken
            
            if (token != null) {
                settingsRepository.updateGithubPat(token)
                AuthorizationResult.Success(token = token, message = "GitHub delegated auth successful")
            } else {
                AuthorizationResult.Error("Token was null in credential")
            }
        } catch (e: Exception) {
            Log.e("GitHubConnectionProvider", "Auth failed", e)
            AuthorizationResult.Error("GitHub auth failed: ${e.message}")
        }
    }

    override suspend fun verify(): VerificationResult {
        val pat = settingsRepository.githubPatFlow.firstOrNull()
        if (pat.isNullOrEmpty()) return VerificationResult.Error("No token available")
        return VerificationResult.Success("GitHub token verified locally")
    }

    override suspend fun refresh(): AuthorizationResult {
        return authenticate(null) // typically would refresh token silently
    }

    override suspend fun revoke() {
        settingsRepository.updateGithubPat("")
    }
}
