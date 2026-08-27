package com.example.ai.capabilities

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import com.example.network.*

class Phase8IntegrationTest {

    // 1. Mocking the API layer so we can run the test offline without real tokens,
    //    but proving the Retrofit / physical boundary structure works.
    class MockGitHubApiService : GitHubApiService {
        var createBranchCalled = false
        var createPullRequestCalled = false
        
        override suspend fun getWorkflowRuns(auth: String?, owner: String, repo: String, perPage: Int) = GitHubWorkflowRunsResponse(0, emptyList())
        override suspend fun getRepoTree(auth: String?, owner: String, repo: String, branch: String) = GitHubTreeResponse("", "", emptyList(), false)
        override suspend fun downloadFile(url: String, auth: String?, accept: String) = okhttp3.ResponseBody.create(null, "")
        override suspend fun getReference(auth: String?, owner: String, repo: String, branch: String) = GitHubRefResponse("refs/heads/main", GitHubRefObject("sha123", "commit"))
        override suspend fun getCommit(auth: String?, owner: String, repo: String, sha: String) = GitHubCommitResponse("sha123", GitHubCommitTree("tree123"))
        override suspend fun createBlob(auth: String?, owner: String, repo: String, request: GitHubBlobRequest) = GitHubBlobResponse("sha123", "")
        override suspend fun createTree(auth: String?, owner: String, repo: String, request: GitHubTreeRequest) = GitHubTreeResponseInfo("sha123", "")
        override suspend fun createCommit(auth: String?, owner: String, repo: String, request: GitHubCreateCommitRequest) = GitHubCommitResponse("sha123", GitHubCommitTree("tree123"))
        override suspend fun updateReference(auth: String?, owner: String, repo: String, branch: String, request: GitHubUpdateRefRequest) = GitHubRefResponse("refs/heads/$branch", GitHubRefObject("sha123", "commit"))
        
        override suspend fun listRepositories(auth: String?) = listOf(GitHubRepoDto(GitHubOwnerDto("testuser"), "testrepo", "main", "test", 1, "Kotlin"))
        override suspend fun getRepository(auth: String?, owner: String, repo: String) = GitHubRepoDto(GitHubOwnerDto("testuser"), "testrepo", "main", "test", 1, "Kotlin")
        override suspend fun getIssue(auth: String?, owner: String, repo: String, issueNumber: Int) = GitHubIssueDto(1, "Fix the bug", "UI flickers", "open")
        
        override suspend fun createReference(auth: String?, owner: String, repo: String, request: GitHubCreateRefRequest): GitHubRefResponse {
            createBranchCalled = true
            return GitHubRefResponse(request.ref, GitHubRefObject(request.sha, "commit"))
        }
        
        override suspend fun createPullRequest(auth: String?, owner: String, repo: String, request: GitHubCreatePRRequest): GitHubPRResponse {
            createPullRequestCalled = true
            return GitHubPRResponse("https://github.com/testuser/testrepo/pull/1", 1)
        }
    }

    class MockFirebaseSandboxManager : RemoteSandboxManager {
        var provisioned = false
        var destroyed = false
        var lastCommand = ""
        
        override suspend fun provisionSandbox(jobId: String, config: SandboxConfig): String {
            provisioned = true
            return "sandbox-123"
        }
        override suspend fun destroySandbox(sandboxId: String): Boolean {
            destroyed = true
            return true
        }
        override suspend fun cloneRepository(sandboxId: String, repo: RepositoryRef, secureToken: String) = true
        override suspend fun executeCommand(sandboxId: String, command: String, timeoutMinutes: Int): ExecutionResult {
            lastCommand = command
            if (command.contains("fail")) return ExecutionResult(1, "", "Failed", false)
            return ExecutionResult(0, "Success", "", false)
        }
    }

    class MockJobStateRepository : JobStateRepository {
        var status = "QUEUED"
        override suspend fun updateJobState(jobId: String, newStatus: String, resultMessage: String?): Boolean {
            status = newStatus
            return true
        }
        override suspend fun getJobState(jobId: String) = status
        override suspend fun approveJob(jobId: String) = true
        override suspend fun rejectJob(jobId: String) = true
    }
    
    // Simulate Verifier engine backed by Remote Sandbox
    class SandboxBackedVerifier(private val sandboxManager: RemoteSandboxManager) : RuntimeVerificationEngine {
        override suspend fun build(repo: RepositoryRef, sandboxId: String): BuildResult {
            val res = sandboxManager.executeCommand(sandboxId, "./gradlew assembleDebug", 5)
            return BuildResult(res.exitCode == 0, res.stdout, "/apk")
        }
        override suspend fun launch(repo: RepositoryRef, buildResult: BuildResult): RuntimeSession {
            return RuntimeSession("sess-1", "Android")
        }
        override suspend fun observe(session: RuntimeSession, mode: ObservationMode): RuntimeObservation {
            return RuntimeObservation(mode, "Video evidence captured", System.currentTimeMillis())
        }
        override suspend fun actuate(session: RuntimeSession, action: RuntimeAction): ActionResult {
            return ActionResult(true, null)
        }
        override suspend fun reproduce(session: RuntimeSession, scenario: TestScenario): ReproductionResult {
            return ReproductionResult(true, emptyList())
        }
        override suspend fun diagnose(evidence: RuntimeEvidence): Diagnosis {
            return Diagnosis("Found crash in logcat", "Fix null pointer")
        }
        override suspend fun verifyFix(before: RuntimeEvidence, after: RuntimeEvidence): VerificationResult {
            return VerificationResult(true, "HIGH", "ledger-1")
        }
    }

    @Test
    fun testPhase8CompleteWorkflow() = runBlocking {
        val githubApi = MockGitHubApiService()
        val githubService = GitHubServiceImpl(githubApi, "fake-token")
        val sandboxManager = MockFirebaseSandboxManager()
        val jobState = MockJobStateRepository()
        val verifier = SandboxBackedVerifier(sandboxManager)
        
        val repo = RepositoryRef("testuser", "testrepo")
        
        // 1. Job initialization
        jobState.updateJobState("job-1", "UNDERSTAND", null)
        assertEquals("UNDERSTAND", jobState.getJobState("job-1"))
        
        // 2. Provision sandbox
        val sandboxId = sandboxManager.provisionSandbox("job-1", SandboxConfig(SandboxLimits(1024, 1.0f, 15), NetworkPolicy.ISOLATED, "ubuntu"))
        assertTrue(sandboxManager.provisioned)
        
        // 3. GitHub Inspection
        val metadata = githubService.inspectRepository(repo)
        assertEquals("Kotlin", metadata.languages.first())
        
        // 4. Remote Sandbox Worker Loop
        sandboxManager.cloneRepository(sandboxId, repo, "fake-token")
        
        val issue = githubService.inspectIssue(repo, 1)
        
        // Aider agent edits file
        val worker = AiderRuntime(sandboxManager, sandboxId)
        val modificationSuccess = worker.modify("Fix ${issue.title}")
        assertTrue(modificationSuccess)
        assertEquals("aider --message 'Implement: Fix Fix the bug' --yes", sandboxManager.lastCommand)
        
        // 5. Build in sandbox via verifier
        val buildResult = verifier.build(repo, sandboxId)
        assertTrue(buildResult.success)
        assertEquals("./gradlew assembleDebug", sandboxManager.lastCommand) // Last command from build
        
        // 6. Test runtime verification
        val session = verifier.launch(repo, buildResult)
        val evidence = RuntimeEvidence(listOf(verifier.observe(session, ObservationMode.VIDEO)), false)
        val diagnosis = verifier.diagnose(evidence)
        assertEquals("Fix null pointer", diagnosis.suggestedFix)
        
        // Verification loop complete and approved
        val verified = verifier.verifyFix(evidence, RuntimeEvidence(emptyList(), true))
        assertTrue(verified.verified)
        
        // 7. Branch and PR
        githubService.createBranch(repo, "fix-branch")
        assertTrue(githubApi.createBranchCalled)
        
        githubService.createPullRequest(repo, PRDetails("Fix bug", "Fixed it", "fix-branch", "main"))
        assertTrue(githubApi.createPullRequestCalled)
        
        // 8. Cleanup
        sandboxManager.destroySandbox(sandboxId)
        assertTrue(sandboxManager.destroyed)
        
        jobState.updateJobState("job-1", "COMPLETED", "Success")
        assertEquals("COMPLETED", jobState.getJobState("job-1"))
    }
}
