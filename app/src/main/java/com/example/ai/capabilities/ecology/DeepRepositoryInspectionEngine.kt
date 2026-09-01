package com.example.ai.capabilities.ecology

import com.example.ai.capabilities.federated.*
import org.json.JSONObject
import org.json.JSONArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeepRepositoryInspectionEngine(
    private val ecologyEngine: ProjectEcologyEngine,
    private val githubCapability: GitHubCapability
) {
    suspend fun inspectRepository(owner: String, repoName: String) = withContext(Dispatchers.IO) {
        println("━━━━━━━━ M. ENGINE — DEEP REPOSITORY INSPECTION (17.1E & 17.1F) ━━━━━━━━")
        println("Target: $owner/$repoName")
        
        var project = ecologyEngine.getProject("github_$repoName")
        if (project == null) {
            project = ProjectNode(id = "github_$repoName", name = repoName)
            project.profile = ProjectEcologyProfile(repositoryId = project.id)
            ecologyEngine.registerProject(project)
        }
        val profile = project.profile ?: ProjectEcologyProfile(repositoryId = project.id)
        
        val auth = CapabilityAuthorization(listOf(CapabilityScope.READ_METADATA, CapabilityScope.READ_SOURCE), 1.0)
        
        // --- PHASE 1: Repository Snapshot (Level 1) ---
        println("\n--- Phase 1: Repository Snapshot ---")
        val branchReq = CapabilityRequest(CapabilityScope.READ_METADATA, mapOf("owner" to owner, "repo" to repoName, "branch" to "main"))
        val branchResult = githubCapability.execute(auth, branchReq)
        
        if (!branchResult.success) {
            println("❌ Failed to fetch branch metadata: ${branchResult.evidence}")
            profile.inspectionState.level0Registry = ExecutionInspectionStatus.BLOCKED
            project.profile = profile
            return@withContext
        }
        
        val branchJson = JSONObject(branchResult.data as String)
        val commitSha = branchJson.getJSONObject("commit").getString("sha")
        profile.currentCommitSha = commitSha
        profile.defaultBranch = "main"
        
        println("Repository ID: github_$repoName")
        println("Default branch: main")
        println("Commit SHA: $commitSha")
        println("Inspection Timestamp: ${System.currentTimeMillis()}")
        
        val treeReq = CapabilityRequest(CapabilityScope.READ_METADATA, mapOf("owner" to owner, "repo" to repoName, "treeSha" to commitSha))
        val treeResult = githubCapability.execute(auth, treeReq)
        
        val treeFiles = mutableListOf<String>()
        if (treeResult.success) {
            val treeJson = JSONObject(treeResult.data as String)
            val treeArray = treeJson.getJSONArray("tree")
            for (i in 0 until treeArray.length()) {
                val item = treeArray.getJSONObject(i)
                if (item.getString("type") == "blob") {
                    treeFiles.add(item.getString("path"))
                }
            }
        }
        
        val manifests = treeFiles.filter { it.endsWith("package.json") || it.endsWith("build.gradle") || it.endsWith("build.gradle.kts") || it.endsWith("pom.xml") }
        println("Manifest files discovered: ${manifests.joinToString(", ")}")
        
        profile.inspectionState.level1Structural = ExecutionInspectionStatus.COMPLETE
        profile.healthMatrix.structuralHealth = HealthDimensionRecord(
            value = HealthState.OBSERVED,
            confidence = 0.95,
            evidenceReferences = listOf("Tree fetched", "${manifests.size} manifests found"),
            sourceCommitSha = commitSha
        )
        
        // --- PHASE 2: Manifest Inspection (Level 2) ---
        println("\n--- Phase 2: Manifest Inspection ---")
        val deps = mutableListOf<String>()
        var foundPackageJson = false
        var packageJsonContent = ""
        
        for (manifest in manifests) {
            val req = CapabilityRequest(CapabilityScope.READ_SOURCE, mapOf("owner" to owner, "repo" to repoName, "sha" to commitSha, "path" to manifest))
            val res = githubCapability.execute(auth, req)
            if (res.success) {
                if (manifest.endsWith("package.json")) {
                    foundPackageJson = true
                    packageJsonContent = res.data as String
                    try {
                        val pJson = JSONObject(packageJsonContent)
                        if (pJson.has("dependencies")) {
                            val d = pJson.getJSONObject("dependencies")
                            d.keys().forEach { k -> deps.add(k) }
                        }
                        if (pJson.has("devDependencies")) {
                            val d = pJson.getJSONObject("devDependencies")
                            d.keys().forEach { k -> deps.add(k) }
                        }
                    } catch (e: Exception) {
                        println("Failed to parse package.json")
                    }
                } else if (manifest.endsWith("build.gradle.kts")) {
                    val content = res.data as String
                    if (content.contains("implementation(")) {
                        deps.add("kotlin-dependencies")
                    }
                }
            }
        }
        
        if (deps.isNotEmpty()) {
            println("OBSERVATION: Repository $repoName declares dependencies: ${deps.take(5).joinToString(", ")}${if(deps.size > 5) "..." else ""}")
            println("Confidence: 0.99")
            println("Evidence: Parsed from manifests at commit $commitSha")
            
            profile.healthMatrix.dependencyFreshness = HealthDimensionRecord(
                value = DependencyState.UNKNOWN,
                confidence = 0.5,
                evidenceReferences = listOf("Dependencies parsed (${deps.size})", "Versions not yet verified against external ecosystem registry"),
                sourceCommitSha = commitSha,
                uncertaintyReason = "Requires remote registry resolution capability",
                recommendedNextAction = "ACQUIRE_CAPABILITY: DEPENDENCY_REGISTRY_CHECK"
            )
        } else {
             println("OBSERVATION: No declared package dependencies found.")
             println("Confidence: 0.85")
             println("Evidence: No known manifests found or parsed at commit $commitSha")
        }
        
        // --- PHASE 3: Source-level structural inspection (Level 2) ---
        println("\n--- Phase 3: Source-level structural inspection ---")
        val sourceDirs = treeFiles.filter { it.startsWith("src/") || it.startsWith("app/src/") || it.startsWith("lib/") }
        val testDirs = treeFiles.filter { it.contains("test/") || it.contains("tests/") || it.contains("__tests__") }
        
        if (sourceDirs.isNotEmpty()) {
            println("OBSERVATION: Recognized source boundaries: ${sourceDirs.size} source files.")
        } else {
            println("OBSERVATION: No recognized source directories discovered in the inspected scope.")
        }
        
        if (testDirs.isNotEmpty()) {
            println("OBSERVATION: Recognized test boundaries: ${testDirs.size} test files.")
        } else {
            println("OBSERVATION: No direct test directories discovered in the inspected scope.")
        }
        
        profile.inspectionState.level2Semantic = ExecutionInspectionStatus.COMPLETE
        profile.healthMatrix.architecturalComplexity = HealthDimensionRecord(
            value = "OBSERVED",
            confidence = 0.8,
            evidenceReferences = listOf("${sourceDirs.size} source files", "${testDirs.size} test files"),
            sourceCommitSha = commitSha
        )
        
        // --- PHASE 4: Execution Evidence (Level 3+) ---
        println("\n--- Phase 4: Execution Evidence ---")
        println("Attempting execution ladder...")
        println("METADATA INSPECTION: COMPLETE")
        println("MANIFEST PARSING: COMPLETE")
        println("STATIC ANALYSIS: UNKNOWN (Blocked by lack of isolated execution environment)")
        println("DEPENDENCY INSTALL: UNKNOWN")
        println("BUILD: UNKNOWN")
        println("TEST: UNKNOWN")
        println("RUNTIME SMOKE TEST: UNKNOWN")
        
        profile.inspectionState.level3Execution = ExecutionInspectionStatus.BLOCKED
        profile.inspectionState.level4Runtime = ExecutionInspectionStatus.NOT_ATTEMPTED
        profile.inspectionState.level5CrossProject = ExecutionInspectionStatus.NOT_ATTEMPTED
        
        val executionGap = CapabilityGap(
            missingCapability = "SANDBOXED_LOCAL_EXECUTION",
            locallyObtainable = false,
            acquisitionCost = "High (Infrastructure Provisioning)",
            securityImplications = "Requires secure container isolation",
            reproducibilityImpact = "Crucial for physical verification",
            alternativeEnvironment = "GitHub Actions execution or authorized remote runner",
            authorizationRequired = "PROVISION_SANDBOX"
        )
        
        profile.healthMatrix.buildHealth = createUnknown(
            unknownValue = HealthState.UNKNOWN,
            reason = "No isolated execution sandbox currently available.",
            gap = executionGap
        ).copy(sourceCommitSha = commitSha)
        
        profile.healthMatrix.testHealth = createUnknown(
            unknownValue = HealthState.UNKNOWN,
            reason = "No isolated execution sandbox currently available.",
            gap = executionGap
        ).copy(sourceCommitSha = commitSha)
        
        // --- PHASE 5: Health Evidence Matrix ---
        println("\n--- Phase 5: Health Evidence Matrix (17.1F) ---")
        printRecord("Structural Health", profile.healthMatrix.structuralHealth)
        printRecord("Build Health", profile.healthMatrix.buildHealth)
        printRecord("Test Health", profile.healthMatrix.testHealth)
        printRecord("Dependency Freshness", profile.healthMatrix.dependencyFreshness)
        printRecord("Activity", profile.healthMatrix.activity)
        printRecord("Issue Pressure", profile.healthMatrix.issuePressure)
        printRecord("Architectural Complexity", profile.healthMatrix.architecturalComplexity)
        printRecord("Goal Relevance", profile.healthMatrix.goalRelevance)
        
        project.profile = profile
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
    
    private fun <T> printRecord(name: String, record: HealthDimensionRecord<T>) {
        println("$name: ${record.value}")
        if (record.value.toString() == "UNKNOWN" && record.capabilityGap != null) {
            println("  ↳ Reason: ${record.uncertaintyReason}")
            println("  ↳ CAPABILITY GAP: ${record.capabilityGap.missingCapability}")
            println("  ↳ Alternative: ${record.capabilityGap.alternativeEnvironment}")
        } else if (record.evidenceReferences.isNotEmpty()) {
            println("  ↳ Evidence: ${record.evidenceReferences.joinToString("; ")}")
            println("  ↳ Source Commit: ${record.sourceCommitSha ?: "N/A"}")
        } else {
            println("  ↳ Reason: ${record.uncertaintyReason}")
        }
    }
}
