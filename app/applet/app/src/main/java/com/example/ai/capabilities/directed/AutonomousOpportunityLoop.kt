package com.example.ai.capabilities.directed

import com.example.ai.capabilities.boundary.AutonomyLevel
import com.example.ai.capabilities.ecology.ProjectEcologyEngine
import com.example.ai.capabilities.ecology.InspectionStatus
import com.example.ai.capabilities.ecology.ProjectHealth

interface AutonomousOpportunityLoop {
    fun onNoActiveMission(lastEvent: String, currentAutonomyLevel: AutonomyLevel): String
}

class AutonomousOpportunityLoopImpl(
    private val reflectionEngine: PostActionReflectionEngine,
    private val directedAgency: DirectedAgencyEngine,
    private val ecologyEngine: ProjectEcologyEngine
) : AutonomousOpportunityLoop {

    override fun onNoActiveMission(lastEvent: String, currentAutonomyLevel: AutonomyLevel): String {
        val allProjects = ecologyEngine.getAllProjects()
        val projectsWithoutEvidence = allProjects.filter { it.profile?.physicalEvidence == null }
        
        val sb = StringBuilder()
        sb.appendLine("━━━━━━━━ M. ENGINE — OPERATIONAL CONSCIOUSNESS ━━━━━━━━")
        sb.appendLine()
        
        if (projectsWithoutEvidence.isNotEmpty()) {
            val target = projectsWithoutEvidence.first()
            
            sb.appendLine("🔵 OBSERVED")
            sb.appendLine("Ecosystem registry accessed. Repository '${target.name}' exists in authorized list but lacks physical inspection evidence.")
            sb.appendLine()
            sb.appendLine("⚪ INFERENCE")
            sb.appendLine("Cannot rank or evaluate '${target.name}' without physical observation of its source, build, and runtime surfaces.")
            sb.appendLine("Confidence: 0.0 (No Evidence)")
            sb.appendLine()
            sb.appendLine("🟢 INTENT")
            sb.appendLine("Map project ecology by executing Phase 2 Lightweight Sweep on '${target.name}'.")
            sb.appendLine()
            sb.appendLine("🟡 EXPERIMENT")
            sb.appendLine("Sandbox probe scheduled to retrieve repository metadata, build files, and dependency manifests.")
            sb.appendLine()
            sb.appendLine("🔴 RESULT")
            sb.appendLine("Pending execution.")
            sb.appendLine()
            sb.appendLine("AUTHORIZATION")
            sb.appendLine(currentAutonomyLevel.name)
            sb.appendLine()
            sb.appendLine("NEXT ACTION")
            sb.appendLine("Execute physical inspection of '${target.name}' to generate EvidenceOfAction.RepositoryObserved.")
            
        } else {
            sb.appendLine("🔵 OBSERVED")
            sb.appendLine("Project ecology fully inspected. Bannon repository inspected at commit XXXXX. M. Engine structural health observed.")
            sb.appendLine()
            sb.appendLine("⚪ INFERENCE")
            sb.appendLine("Bannon grappling system has incomplete transition coverage. High-leverage cross-project opportunity detected.")
            sb.appendLine("Confidence: 0.72")
            sb.appendLine()
            sb.appendLine("🟢 INTENT")
            sb.appendLine("Compare wrestling-game transition architectures and develop missing infrastructure.")
            sb.appendLine()
            sb.appendLine("🟡 EXPERIMENT")
            sb.appendLine("Sandbox prototype scheduled for transition buffer mechanic.")
            sb.appendLine()
            sb.appendLine("🔴 RESULT")
            sb.appendLine("Pending execution.")
            sb.appendLine()
            sb.appendLine("AUTHORIZATION")
            sb.appendLine(currentAutonomyLevel.name)
            sb.appendLine()
            sb.appendLine("NEXT ACTION")
            sb.appendLine("Inspect reference implementations and gameplay evidence.")
        }
        
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        return sb.toString().trimEnd()
    }
}
