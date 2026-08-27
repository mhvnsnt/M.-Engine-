package com.example.ai.capabilities

enum class ProjectType {
    ANDROID, GODOT, UNITY, UNREAL, WEB_REACT, NODE_BACKEND, GENERIC
}

data class ProjectIntelligence(
    val type: ProjectType,
    val primaryLanguage: String,
    val buildSystem: String,
    val hasTests: Boolean,
    val hasCI: Boolean,
    val entryPoints: List<String>,
    val knownFragileSystems: List<String> = emptyList()
)

interface RepositoryIntelligenceEngine {
    suspend fun analyzeRepository(repo: RepositoryRef, sandboxId: String): ProjectIntelligence
    suspend fun suggestVerificationProfile(intelligence: ProjectIntelligence): VerificationProfile
}

data class VerificationProfile(
    val requiresRuntimeTesting: Boolean,
    val requiresVisualTesting: Boolean,
    val requiresInteractionTesting: Boolean,
    val expectedObservationModes: List<ObservationMode>
)
