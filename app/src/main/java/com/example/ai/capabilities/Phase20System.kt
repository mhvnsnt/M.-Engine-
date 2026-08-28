package com.example.ai.capabilities.phase20

import java.io.File

/**
 * Phase 20: M. Engine Bootstrap & Live Reality Loop
 * This package enforces the "Models propose. Reality decides." architecture
 * without breaking legacy mock implementations in `capabilities/`.
 */

enum class RealityEvidenceLevel(val value: Int) {
    MODEL_CLAIM(0),               // suggestion
    STATIC_ANALYSIS(1),           // weak evidence
    COMPILER_SUCCESS(2),          // build evidence
    UNIT_TEST(3),                 // test evidence
    INTEGRATION_TEST(4),          // stronger evidence
    RUNTIME_OBSERVATION(5),       // reality evidence
    TEMPORAL_VIDEO_INTERACTION(6),// very strong evidence
    REPRODUCTION_REGRESSION(7)    // strongest behavioral evidence
}

data class RealityEvidenceRecord(
    val id: String,
    val claim: String,
    val level: RealityEvidenceLevel,
    val commitSha: String,
    val environment: String,
    val testId: String?,
    val observationMethod: String,
    val resultStatus: String,
    val rawDataUri: String?,
    val verifiedAt: Long = System.currentTimeMillis()
)

interface StrictEvidenceEngine {
    suspend fun verifyClaim(claim: String, requiredLevel: RealityEvidenceLevel): Boolean
    suspend fun recordEvidence(evidence: RealityEvidenceRecord)
}

// ---------------------------------------------------------
// Observation Routing (Choosing actuator automatically)
// ---------------------------------------------------------

enum class BugType { VISUAL, BEHAVIORAL, CRASH, PERFORMANCE, GAME_LOGIC, UNKNOWN }
enum class ActuatorType {
    SCREENSHOT, UI_HIERARCHY, ACCESSIBILITY_TREE, TEMPORAL_VIDEO,
    SCRIPTED_INTERACTION, STATE_TRANSITION_TRACE, LOGCAT, STACK_TRACE,
    CPU_MEMORY_PROFILER, GAME_INPUT_SEQUENCE
}

object AutomaticObservationRouter {
    fun route(bugType: BugType): List<ActuatorType> {
        return when (bugType) {
            BugType.VISUAL -> listOf(ActuatorType.SCREENSHOT, ActuatorType.UI_HIERARCHY, ActuatorType.ACCESSIBILITY_TREE, ActuatorType.TEMPORAL_VIDEO)
            BugType.BEHAVIORAL -> listOf(ActuatorType.SCRIPTED_INTERACTION, ActuatorType.TEMPORAL_VIDEO, ActuatorType.STATE_TRANSITION_TRACE)
            BugType.CRASH -> listOf(ActuatorType.LOGCAT, ActuatorType.STACK_TRACE)
            BugType.PERFORMANCE -> listOf(ActuatorType.CPU_MEMORY_PROFILER, ActuatorType.TEMPORAL_VIDEO)
            BugType.GAME_LOGIC -> listOf(ActuatorType.GAME_INPUT_SEQUENCE, ActuatorType.TEMPORAL_VIDEO, ActuatorType.STATE_TRANSITION_TRACE)
            BugType.UNKNOWN -> listOf(ActuatorType.LOGCAT, ActuatorType.SCREENSHOT)
        }
    }
}

// ---------------------------------------------------------
// Personal Context Engine (Modeling the user)
// ---------------------------------------------------------

enum class MemoryProvenance { EXPLICIT, OBSERVED, INFERRED, CONFIRMED, REJECTED }
enum class ContextFacet { HOW_YOU_THINK, HOW_YOU_BUILD, GOALS, REPOSITORIES }

data class UserContextMemory(
    val id: String,
    val facet: ContextFacet,
    val provenance: MemoryProvenance,
    val content: String,
    val relatedProjects: List<String>
)

// ---------------------------------------------------------
// First-Class GitHub Connector (OAuth/App over PAT)
// ---------------------------------------------------------

interface GitHubOAuthConnector {
    suspend fun initiateOAuthFlow(): String
    suspend fun handleOAuthCallback(code: String): Boolean
    suspend fun getAppInstallationToken(installationId: String): String
}
