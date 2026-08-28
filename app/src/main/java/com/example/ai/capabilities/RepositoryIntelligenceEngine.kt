package com.example.ai.capabilities

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

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
    val capabilitiesFound: List<String> = emptyList()
)

interface RepositoryIntelligenceEngine {
    suspend fun analyzeRepository(repoDir: File): ProjectIntelligence
    suspend fun buildCapabilityGraph(repoDir: File): List<String>
}

class RepositoryIntelligenceEngineImpl : RepositoryIntelligenceEngine {
    
    override suspend fun analyzeRepository(repoDir: File): ProjectIntelligence = withContext(Dispatchers.IO) {
        val hasGradle = File(repoDir, "build.gradle.kts").exists() || File(repoDir, "build.gradle").exists()
        val hasGodot = File(repoDir, "project.godot").exists()
        
        val type = when {
            hasGodot -> ProjectType.GODOT
            hasGradle -> ProjectType.ANDROID
            else -> ProjectType.GENERIC
        }
        
        val hasTests = File(repoDir, "app/src/test").exists() || File(repoDir, "app/src/androidTest").exists()
        val hasCI = File(repoDir, ".github/workflows").exists()
        
        val capabilities = buildCapabilityGraph(repoDir)
        
        ProjectIntelligence(
            type = type,
            primaryLanguage = if (type == ProjectType.ANDROID) "Kotlin" else "Unknown",
            buildSystem = if (hasGradle) "Gradle" else "Unknown",
            hasTests = hasTests,
            hasCI = hasCI,
            entryPoints = listOf("MainActivity.kt"),
            capabilitiesFound = capabilities
        )
    }

    override suspend fun buildCapabilityGraph(repoDir: File): List<String> = withContext(Dispatchers.IO) {
        val capabilities = mutableListOf<String>()
        val fileTree = repoDir.walkTopDown().filter { it.isFile && it.extension == "kt" }
        
        for (file in fileTree) {
            val content = file.readText()
            if (content.contains("class CodeJarvis") || content.contains("interface CapabilityProvider")) capabilities.add("AI_CODING")
            if (content.contains("class AppActuator") || content.contains("fun executeAdb")) capabilities.add("PHYSICAL_ACTUATION")
            if (content.contains("class EvidenceEngine") || content.contains("EvidenceRecord")) capabilities.add("EVIDENCE_ENGINE")
            if (content.contains("class FailureObservatory")) capabilities.add("FAILURE_OBSERVATORY")
            if (content.contains("class JobManager") || content.contains("JobEntity")) capabilities.add("DURABLE_JOBS")
            if (content.contains("GitHubService") || content.contains("GitHubApiService")) capabilities.add("GITHUB_INTEGRATION")
            if (content.contains("interface CiCdPipeline")) capabilities.add("CI_CD_PIPELINE")
            if (content.contains("class FirebaseSandboxManager")) capabilities.add("FIREBASE_INTEGRATION")
        }
        
        capabilities.distinct()
    }
}
