package com.example.ai.capabilities

enum class AcquisitionResultStatus {
    SECURITY_REJECTED,
    BENCHMARK_FAILED,
    INFERIOR_CAPABILITY,
    PR_CREATED_WAITING_APPROVAL,
    ERROR
}

data class AcquisitionResult(
    val status: AcquisitionResultStatus,
    val candidate: ResearchCandidate,
    val message: String,
    val prUrl: String? = null
)

interface AcquisitionEngine {
    suspend fun scoutEcosystem(objective: String): List<ResearchCandidate>
    suspend fun evaluateAndPropose(
        candidate: ResearchCandidate, 
        capabilityName: String
    ): AcquisitionResult
}

class AcquisitionEngineImpl(
    private val githubService: GitHubService,
    private val sandboxManager: RemoteSandboxManager,
    private val securityScanner: SecurityScanner,
    private val verificationEngine: RuntimeVerificationEngine,
    private val harvestMatrix: CapabilityHarvestMatrix,
    private val capabilityBenchmark: CapabilityBenchmark,
    private val evidenceAssuranceEngine: EvidenceAssuranceEngine
) : AcquisitionEngine {

    override suspend fun scoutEcosystem(objective: String): List<ResearchCandidate> {
        return emptyList()
    }

    override suspend fun evaluateAndPropose(
        candidate: ResearchCandidate, 
        capabilityName: String
    ): AcquisitionResult {
        val targetRepo = RepositoryRef(candidate.sourceType, candidate.name)
        
        // 1. Security Scan
        val securityResult = securityScanner.scanRepository(targetRepo, "sandbox-temp")
        if (!securityResult.passed) {
            return AcquisitionResult(
                AcquisitionResultStatus.SECURITY_REJECTED, 
                candidate, 
                "Rejected due to security violations: ${securityResult.violations.map { it.reason }}"
            )
        }

        // 2. Capability Benchmark (replaces loose pseudo-score)
        val currentImpl = harvestMatrix.getCurrentImplementation(capabilityName)
        val testDef = BenchmarkTestDefinition(
            id = "bench-${capabilityName.lowercase()}",
            description = "Standard regression test for $capabilityName",
            targetRepo = targetRepo,
            executionScenario = TestScenario("scen-1", "Standard Flow", emptyList())
        )
        
        val comparison = capabilityBenchmark.compare(currentImpl, candidate, testDef)
        
        if (!comparison.candidateMetrics.buildSuccess) {
            return AcquisitionResult(AcquisitionResultStatus.BENCHMARK_FAILED, candidate, "Candidate failed to build.")
        }
        
        val candidateRecord = ImplementationRecord(
            capabilityName, candidate.name, candidate.url, comparison.candidateMetrics.effectivenessScore, "ledger-${candidate.id}"
        )

        // 3. Compare with Harvest Matrix
        val decision = harvestMatrix.compareCapabilities(currentImpl, candidateRecord)
        
        if (!decision.shouldIntegrate || !comparison.isSuperior) {
            return AcquisitionResult(
                AcquisitionResultStatus.INFERIOR_CAPABILITY, 
                candidate, 
                "Candidate rejected: ${decision.reason} / Delta: ${comparison.deltaScore}"
            )
        }

        // 4. Create Integration Branch and PR
        val branchName = "integrate/${capabilityName.lowercase()}-${candidate.name}"
        val myEngineRepo = RepositoryRef("mhvnsnt", "M.-Engine-", "main")
        
        githubService.createBranch(myEngineRepo, branchName)
        githubService.commitAndPush(myEngineRepo, "Integrate ${candidate.name}", "Integrate improved $capabilityName capability.")
        
        val prDetails = PRDetails(
            title = "Integration: Upgrade $capabilityName with ${candidate.name}",
            body = "Benchmark proved superior: +${decision.scoreDelta} points.\nEvidence Ledger: ${candidateRecord.evidenceLedgerId}",
            headBranch = branchName,
            baseBranch = "main"
        )
        
        val prId = githubService.createPullRequest(myEngineRepo, prDetails)

        return AcquisitionResult(
            AcquisitionResultStatus.PR_CREATED_WAITING_APPROVAL,
            candidate,
            "Capability verified. PR created for human approval.",
            prId
        )
    }
}
