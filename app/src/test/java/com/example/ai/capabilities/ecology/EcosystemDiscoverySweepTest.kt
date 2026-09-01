package com.example.ai.capabilities.ecology

import com.example.ai.capabilities.directed.EvaluatedOpportunity
import com.example.ai.capabilities.directed.OpportunityCompetitionEngine
import com.example.ai.capabilities.federated.GitHubCapability
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
class EcosystemDiscoverySweepTest {

    @Test
    fun executeRealSweepAndCompetition() {
        val ecologyEngine = ProjectEcologyEngineImpl()
        val githubCapability = GitHubCapability()
        val sweep = EcosystemDiscoverySweep(ecologyEngine, githubCapability)
        
        runBlocking {
            // Mission 17.1D - Cross-Project Dependency Graph (via Tier 1 Sweep)
            sweep.executeTier1Sweep("mhvnsnt")
            
            // Mission 17.1D.5 - Evidence Reconciliation
            println("━━━━━━━━ M. ENGINE — EVIDENCE RECONCILIATION (PHASE 17.1D.5) ━━━━━━━━")
            val reconciliationEngine = EvidenceReconciliationEngine()
            
            // Simulating a contradiction between documentation and structure
            val testRelation = DependencyRelationship(
                sourceId = "github_ProjectA",
                targetId = "github_ProjectB",
                relationshipType = ProjectRelationship.DEPENDS_ON,
                epistemicClassification = EpistemicClassification.OBSERVATION,
                confidence = 0.90,
                evidence = listOf("README.md claims ProjectA integrates with ProjectB"),
                verificationMethod = "Tier 2 Documentary",
                falsificationCondition = null,
                status = EdgeStatus.ACTIVE
            )
            
            val contradiction = reconciliationEngine.reconcile(
                relationship = testRelation,
                documentaryEvidence = listOf("README.md states: 'ProjectA integrates tightly with ProjectB'"),
                structuralEvidence = listOf("No active integration or imports found in package.json or source tree.")
            )
            
            if (contradiction != null) {
                reconciliationEngine.printContradiction(contradiction)
                println("Edge Status updated to: ${testRelation.status}")
                println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
            }
            
            // Mission 17.1C - Opportunity Competition
            val competitionEngine = OpportunityCompetitionEngine(ecologyEngine)
            
            val candidates = listOf(
                EvaluatedOpportunity(
                    title = "Wrestling mechanics transition research and buffer prototype",
                    targetProjectId = "Bannon",
                    ownerGoalAlignment = 0.85,
                    expectedLeverage = 0.9,
                    evidenceConfidence = 0.72,
                    crossProjectImpact = 0.5,
                    reversibility = 0.9,
                    cost = 0.3,
                    risk = 0.1,
                    opportunityCost = 0.4,
                    evidence = "Repository inspected, gameplay references correlated."
                ),
                EvaluatedOpportunity(
                    title = "Extract authentication abstraction into shared ecosystem module",
                    targetProjectId = "God-Mode-OS",
                    ownerGoalAlignment = 0.9,
                    expectedLeverage = 0.8,
                    evidenceConfidence = 0.61, // Backed by Evidence
                    crossProjectImpact = 0.8,
                    reversibility = 0.7,
                    cost = 0.4,
                    risk = 0.2,
                    opportunityCost = 0.5,
                    evidence = "Commit SHA, file paths, and symbol relationships observed in Tier 1."
                ),
                EvaluatedOpportunity(
                    title = "Fix isolated UI rendering glitch in settings panel",
                    targetProjectId = "bolt.diy-M",
                    ownerGoalAlignment = 0.3,
                    expectedLeverage = 0.1,
                    evidenceConfidence = 0.95,
                    crossProjectImpact = 0.1,
                    reversibility = 0.9,
                    cost = 0.1,
                    risk = 0.05,
                    opportunityCost = 0.9,
                    evidence = "Observed visual layout anomaly in DOM structure."
                )
            )
            
            competitionEngine.evaluateOpportunities(candidates)
        }
    }
}
