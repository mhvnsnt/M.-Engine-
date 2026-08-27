package com.example.ai.capabilities

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.example.data.CapabilityKnowledgeDao
import com.example.data.CapabilityKnowledgeEntity
import com.example.network.GitHubRepoDto
import com.example.network.GitHubOwnerDto

class Phase9IntegrationTest {

    class MockGitHubService : GitHubService {
        override suspend fun authenticate(secureToken: String): Boolean = true
        override suspend fun listRepositories(): List<RepositoryRef> = emptyList()
        
        override suspend fun searchCode(repo: RepositoryRef, query: String): List<String> {
            return listOf("https://github.com/microsoft/playwright", "https://github.com/appium/appium")
        }
        
        override suspend fun inspectRepository(repo: RepositoryRef): RepoMetadata {
            return RepoMetadata(
                languages = listOf("Kotlin"),
                defaultBranch = "main",
                description = "Test repo with database and api",
                stars = 10
            )
        }
        
        override suspend fun inspectIssue(repo: RepositoryRef, issueNumber: Int): IssueDetails {
            return IssueDetails(1, "Mock Issue", "Mock Body", "open")
        }
        
        override suspend fun inspectCIResults(repo: RepositoryRef, commitSha: String): CIResult {
            return CIResult("1", true, "url")
        }
        
        override suspend fun retrieveReviewComments(repo: RepositoryRef, prNumber: Int): List<String> = emptyList()
        override suspend fun cloneToRemoteSandbox(repo: RepositoryRef, sandboxId: String): Boolean = true
        override suspend fun createBranch(repo: RepositoryRef, branchName: String): Boolean = true
        override suspend fun commitAndPush(repo: RepositoryRef, patch: String, message: String): Boolean = true
        override suspend fun createPullRequest(repo: RepositoryRef, details: PRDetails): String = "http://pr"
    }

    class MockCapabilityKnowledgeDao : CapabilityKnowledgeDao {
        val storage = mutableMapOf<String, CapabilityKnowledgeEntity>()
        override suspend fun insertKnowledge(entity: CapabilityKnowledgeEntity) {
            storage[entity.capabilityName] = entity
        }
        override suspend fun getKnowledge(capabilityName: String): CapabilityKnowledgeEntity? {
            return storage[capabilityName]
        }
        override suspend fun getAllKnowledge(): List<CapabilityKnowledgeEntity> = storage.values.toList()
    }

    @Test
    fun testPhase9RecursiveAuditAndResearch() = runBlocking {
        val githubService = MockGitHubService()
        val dao = MockCapabilityKnowledgeDao()
        val auditor = RecursiveRepoAuditorImpl(githubService)
        val researchEngine = ResearchEngineImpl(githubService, dao)

        val myRepos = listOf(
            RepositoryRef("owner", "m-engine"),
            RepositoryRef("owner", "god-mode-os")
        )

        // 1. Audit Workspace
        val inventory = auditor.auditWorkspace(myRepos)
        assertTrue(inventory.any { it.name == "Local Data Persistence" && it.state == InventoryState.ALREADY_EXISTS })
        assertTrue(inventory.any { it.name == "Computer Vision" && it.state == InventoryState.MISSING })

        val visionCapability = inventory.find { it.name == "Computer Vision" }
        assertNotNull(visionCapability)

        // 2. State Diagnosis
        val videoActuationState = auditor.diagnoseCapabilityState(myRepos.first(), "Video Stream Actuation")
        assertEquals(InventoryState.EXPERIMENTAL, videoActuationState)

        // 3. Research Alternatives
        val candidates = researchEngine.discover("UI Automation Video Actuation")
        assertEquals(2, candidates.size)

        // 4. Compare (Internal vs External)
        val internalState = CapabilityInventoryItem(
            id = "video-actuation",
            name = "Video Stream Actuation",
            description = "Click on screen via video frame",
            state = videoActuationState, // EXPERIMENTAL
            implementationRef = "github.com/owner/m-engine"
        )

        val recommendation = researchEngine.compare(internalState, candidates)
        assertNotNull(recommendation.recommendedCandidate)
        assertEquals("External candidate outperformed internal implementation.", recommendation.reason)

        // 5. Check Provenance & Integration Mode
        val evaluation = recommendation.evaluation!!
        assertEquals(IntegrationMode.SOURCE_ADAPTATION, evaluation.recommendedIntegrationMode)
        assertNotNull(evaluation.provenance)
        assertEquals("Automated Evaluation", evaluation.provenance?.selectionReason)

        // 6. Propose Integration
        val success = researchEngine.proposeIntegration(recommendation)
        assertTrue(success)

        // Verify stored in knowledge
        val saved = dao.getKnowledge(recommendation.recommendedCandidate!!.name)
        assertNotNull(saved)
        assertEquals("External candidate outperformed internal implementation.", saved?.reasonForWinning)
    }
}
