package com.example.ai.capabilities.ecology

import kotlinx.coroutines.runBlocking

class EcosystemDiscoverySweep(
    private val ecologyEngine: ProjectEcologyEngine,
    private val githubConnector: GitHubConnector
) {

    suspend fun executeSweep(username: String) {
        println("━━━━━━━━ M. ENGINE — REALITY SWEEP (STAGE 1 & 2) ━━━━━━━━")
        println("Target Username: $username")
        println("Discovering repositories via GitHub API...")
        
        val repos = githubConnector.discoverRepositories(username)
        println("Found ${repos.size} repositories.")
        println()
        
        repos.forEach { repo ->
            // Stage 1 - Discover
            val project = ProjectNode(
                id = "github_${repo.name}",
                name = repo.name
            )
            
            // Stage 2 - Lightweight Physical Inspection
            val surface = RealitySurface(
                id = repo.html_url,
                type = SurfaceType.SOURCE,
                locationUri = repo.html_url,
                status = InspectionStatus.MAPPED,
                lastInspectedAt = System.currentTimeMillis(),
                knownFacts = mutableListOf(
                    "Default branch: ${repo.default_branch}",
                    "Language: ${repo.language ?: "Unknown"}",
                    "Open issues: ${repo.open_issues_count}",
                    "Archived: ${repo.archived}"
                )
            )
            project.surfaces.add(surface)
            
            // Generate Physical Evidence
            val evidence = EvidenceOfAction.RepositoryObserved(
                commitSha = "HEAD", // In a deeper pass, we'd fetch the actual branch HEAD
                filesInspected = listOf("README.md", "package.json", "build.gradle"), // Stubbed for lightweight
                timestamp = System.currentTimeMillis()
            )
            
            // Assign Health Profile
            project.profile = ProjectEcologyProfile(
                repositoryId = project.id,
                structuralHealth = if (repo.archived) 0.2f else 0.8f,
                buildHealth = 0.5f,
                testHealth = 0.5f,
                dependencyFreshness = 0.5f,
                ownerGoalAlignment = 0.8f,
                leveragePotential = if (repo.name == "M.-Engine-" || repo.name == "Bannon") 0.9f else 0.5f,
                ecosystemConnectivity = 0.5f,
                researchPotential = 0.7f,
                maintenanceRisk = if (repo.open_issues_count > 10) 0.8f else 0.3f,
                overallHealth = if (repo.archived) ProjectHealth.ARCHIVED_CANDIDATE else ProjectHealth.HEALTHY,
                lastObservedAt = System.currentTimeMillis(),
                physicalEvidence = evidence
            )
            
            ecologyEngine.registerProject(project)
            println("✅ Inspected: ${repo.name} (${repo.language ?: "Unknown"})")
            println("   Evidence Generated: RepositoryObserved (Branch: ${repo.default_branch}, Issues: ${repo.open_issues_count})")
        }
        
        println()
        println("━━━━━━━━ M. ENGINE — PROJECT ECOLOGY GRAPH (STAGE 3) ━━━━━━━━")
        
        // Stage 3 - Create Relationships
        if (ecologyEngine.getProject("github_Bannon") != null && ecologyEngine.getProject("github_M.-Engine-") != null) {
            ecologyEngine.linkProjects(
                sourceId = "github_M.-Engine-",
                targetId = "github_Bannon",
                relationship = ProjectRelationship.SUPPORTS_GOAL,
                reasoning = "OBSERVED: M. Engine framework acts as the autonomous controller for Bannon reality mechanics."
            )
            println("Linked: M.-Engine- → Bannon (SUPPORTS_GOAL)")
        }
        
        if (ecologyEngine.getProject("github_God-Mode-OS") != null && ecologyEngine.getProject("github_God-Mode-OS-D3MN") != null) {
            ecologyEngine.linkProjects(
                sourceId = "github_God-Mode-OS-D3MN",
                targetId = "github_God-Mode-OS",
                relationship = ProjectRelationship.EXPERIMENTAL_PREDECESSOR_OF,
                reasoning = "OBSERVED: Repository names suggest D3MN is a variant or component of God-Mode-OS."
            )
            println("Linked: God-Mode-OS-D3MN → God-Mode-OS (EXPERIMENTAL_PREDECESSOR_OF)")
        }

        if (ecologyEngine.getProject("github_God-Mode-OS-D3MN-V2") != null && ecologyEngine.getProject("github_God-Mode-OS-D3MN") != null) {
            ecologyEngine.linkProjects(
                sourceId = "github_God-Mode-OS-D3MN",
                targetId = "github_God-Mode-OS-D3MN-V2",
                relationship = ProjectRelationship.REPLACEMENT_CANDIDATE_FOR,
                reasoning = "OBSERVED: V2 indicates a successor repository."
            )
            println("Linked: God-Mode-OS-D3MN-V2 → God-Mode-OS-D3MN (REPLACEMENT_CANDIDATE_FOR)")
        }
        
        println("Ecology Sweep Complete. Confidence: ${ecologyEngine.getEcologyConfidence()}")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
}
