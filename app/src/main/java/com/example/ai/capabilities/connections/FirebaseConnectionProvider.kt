package com.example.ai.capabilities.connections

import android.app.Activity
import com.google.firebase.FirebaseApp

class FirebaseConnectionProvider : ConnectionProvider {
    override val id = "firebase"
    override val name = "Firebase Services"
    override val classification = RealityClassification.REAL_AND_CONNECTED

    override suspend fun discoverCapabilities(): Set<CapabilityType> = setOf(CapabilityType.APP_DISTRIBUTION, CapabilityType.STORAGE)

    override suspend fun healthCheck(): ConnectionStatus {
        return try {
            if (try { FirebaseApp.getInstance(); true } catch (e: Exception) { false }) {
                ConnectionStatus.CONNECTED
            } else {
                ConnectionStatus.UNCONFIGURED
            }
        } catch (e: Exception) {
            ConnectionStatus.ERROR
        }
    }

    override suspend fun connect(activity: Activity?): AuthorizationResult {
        return AuthorizationResult.Success(message = "Firebase is inherent to the application container.")
    }

    override suspend fun disconnect() {
        // Cannot disconnect inherent firebase
    }

    override suspend fun authenticate(activity: Activity?): AuthorizationResult {
        return connect(activity)
    }

    override suspend fun verify(): VerificationResult {
        return try {
            if (try { FirebaseApp.getInstance(); true } catch (e: Exception) { false }) {
                VerificationResult.Success("FirebaseApp is initialized")
            } else {
                VerificationResult.Error("FirebaseApp not initialized")
            }
        } catch (e: Exception) {
            VerificationResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun refresh(): AuthorizationResult {
        return connect(null)
    }

    override suspend fun revoke() {
        // Cannot revoke inherent firebase
    }
}
