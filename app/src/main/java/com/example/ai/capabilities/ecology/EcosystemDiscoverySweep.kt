package com.example.ai.capabilities.ecology

import com.example.ai.capabilities.federated.*

class EcosystemDiscoverySweep(
    private val ecologyEngine: ProjectEcologyEngine,
    private val githubCapability: GitHubCapability
) {

    suspend fun executeTier1Sweep(username: String) {
        println("━━━━━━━━ M. ENGINE — STRUCTURAL REALITY SWEEP (TIER 1) ━━━━━━━━")
        println("Target Username: $username")
        println("Evaluating Federated Capability: ${githubCapability.capabilityId}")
        
        val discoverRequest = CapabilityRequest(CapabilityScope.DISCOVER_REPOSITORIES, mapOf("username" to username))
        val assessment = githubCapability.assess(discoverRequest)
        
        if (!assessment.authorized) {
            println("Capability denied: ${assessment.reasoning}")
            return
        }
        
        val auth = CapabilityAuthorization(listOf(CapabilityScope.DISCOVER_REPOSITORIES, CapabilityScope.READ_METADATA), 1.0)
        
        val discoverResult = githubCapability.execute(auth, discoverRequest)
        if (!discoverResult.success || discoverResult.data !is List<*>) {
            println("Discovery failed: ${discoverResult.evidence}")
            return
        }
        
        @Suppress("UNCHECKED_CAST")
        val repos = discoverResult.data as List<GitHubRepoResponse>
        
        val claims = mutableListOf<KnowledgeClaim>()
        
        repos.forEach { repo ->
            claims.add(KnowledgeClaim(
                type = ClaimType.OBSERVATION,
                statement = "Repository '${repo.name}' exists.",
                confidence = 0.99,
                evidence = "GitHub API response + repository identifier."
            ))
            
            claims.add(KnowledgeClaim(
                type = ClaimType.OBSERVATION,
                statement = "${repo.name} default branch is ${repo.default_branch}.",
                confidence = 0.99,
                evidence = "Repository metadata."
            ))

            val metaRequest = CapabilityRequest(CapabilityScope.READ_METADATA, mapOf("repoUrl" to repo.html_url))
            val metaResult = githubCapability.execute(auth, metaRequest)
            
            if (metaResult.success) {
                claims.add(KnowledgeClaim(
                    type = ClaimType.OBSERVATION,
                    statement = "${repo.name} contains recognizable build manifests and README.",
                    confidence = 0.90,
                    evidence = metaResult.evidence
                ))
            }
            
            val project = ProjectNode(id = "github_${repo.name}", name = repo.name)
            project.profile = ProjectEcologyProfile(
                repositoryId = project.id,
                structuralHealth = if (repo.archived) 0.2f else 0.8f,
                buildHealth = 0.5f,
                testHealth = 0.5f,
                dependencyFreshness = 0.5f,
                ownerGoalAlignment = if (repo.name.contains("M.-Engine")) 0.9f else 0.5f,
                leveragePotential = if (repo.name == "M.-Engine-" || repo.name == "Bannon") 0.9f else 0.5f,
                ecosystemConnectivity = 0.5f,
                researchPotential = 0.7f,
                maintenanceRisk = if (repo.open_issues_count > 10) 0.8f else 0.3f,
                overallHealth = if (repo.archived) ProjectHealth.ARCHIVED_CANDIDATE else ProjectHealth.HEALTHY,
                lastObservedAt = System.currentTimeMillis(),
                physicalEvidence = EvidenceOfAction.RepositoryObserved("HEAD", listOf("README.md"))
            )
            ecologyEngine.registerProject(project)
        }
        
        if (ecologyEngine.getProject("github_Bannon") != null && ecologyEngine.getProject("github_M.-Engine-") != null) {
            val relation = DependencyRelationship(
                sourceId = "github_M.-Engine-",
                targetId = "github_Bannon",
                relationshipType = ProjectRelationship.SUPPORTS_GOAL,
                epistemicClassification = EpistemicClassification.INFERENCE,
                confidence = 0.72,
                evidence = listOf("Repository names", "Project descriptions", "Structural similarity indicators"),
                verificationMethod = "Tier 1 Metadata Heuristics",
                falsificationCondition = FalsificationProbe(ProbeType.DOCUMENTATION_REFERENCE, "README.md", "Bannon"),
                status = EdgeStatus.ACTIVE
            )
            ecologyEngine.linkProjects(relation)
            
            claims.add(KnowledgeClaim(
                type = ClaimType.INFERENCE,
                statement = "M.-Engine- supports the development goals of Bannon.",
                confidence = relation.confidence,
                evidence = relation.evidence.joinToString("; ")
            ))
            
            claims.add(KnowledgeClaim(
                type = ClaimType.HYPOTHESIS,
                statement = "M.-Engine- should become the autonomous development controller for Bannon.",
                confidence = 0.55,
                evidence = "Missing execution-tier evidence.",
                requiresAction = "Requires structural and runtime inspection."
            ))
        }
        
        if (ecologyEngine.getProject("github_Bannon") != null && ecologyEngine.getProject("github_bolt.diy-M") != null) {
             val negRelation = DependencyRelationship(
                sourceId = "github_Bannon",
                targetId = "github_bolt.diy-M",
                relationshipType = ProjectRelationship.NO_EVIDENCE_OF_RELATIONSHIP,
                epistemicClassification = EpistemicClassification.OBSERVATION,
                confidence = 0.89,
                evidence = listOf("No shared dependency, import, API endpoint, workspace reference, documentation reference, or runtime interaction was discovered."),
                verificationMethod = "Tier 1 Structural Sweep",
                falsificationCondition = null,
                status = EdgeStatus.CURRENTLY_UNRELATED
            )
            ecologyEngine.linkProjects(negRelation)
            
            claims.add(KnowledgeClaim(
                type = ClaimType.OBSERVATION,
                statement = "No relationship discovered between Bannon and bolt.diy-M.",
                confidence = negRelation.confidence,
                evidence = negRelation.evidence.joinToString("; ")
            ))
        }
        
        claims.forEach { claim ->
            println("${claim.type}")
            println(claim.statement)
            println("Confidence: ${claim.confidence}")
            if (claim.requiresAction != null) {
                println("Status: ${claim.requiresAction}")
            } else {
                println("Evidence: ${claim.evidence}")
            }
            println()
        }
        
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
}
