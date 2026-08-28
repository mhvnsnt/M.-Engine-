with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "r") as f:
    code = f.read()

import re

comp_func = """
    suspend fun runCapabilityCompetition(capabilityName: String): com.example.ai.capabilities.AcquisitionResult {
        val githubService = com.example.ai.capabilities.GitHubServiceImpl(com.example.network.RetrofitClient.githubService, githubPat.value)
        
        val sandboxManager = com.example.ai.capabilities.FirebaseSandboxManager(
            com.google.firebase.functions.FirebaseFunctions.getInstance()
        )
        val securityScanner = com.example.ai.capabilities.SecurityScannerImpl()
        val verificationEngine = com.example.ai.capabilities.RuntimeVerificationEngineImpl()
        val harvestMatrix = com.example.ai.capabilities.CapabilityHarvestMatrixImpl()
        val capabilityBenchmark = com.example.ai.capabilities.CapabilityBenchmarkImpl(verificationEngine, sandboxManager)
        val evidenceAssuranceEngine = com.example.ai.capabilities.EvidenceAssuranceEngineImpl()
        
        val acquisitionEngine = com.example.ai.capabilities.AcquisitionEngineImpl(
            githubService, sandboxManager, securityScanner, verificationEngine, harvestMatrix, capabilityBenchmark, evidenceAssuranceEngine
        )
        
        val nativeCandidate = com.example.ai.capabilities.ResearchCandidate(
            id = "native", 
            name = "M. Engine Native", 
            sourceType = "GITHUB", 
            url = "local://m-engine", 
            description = "Current internal implementation", 
            versionOrCommit = "main",
            createdAtYear = 2026,
            lastUpdatedYear = 2026,
            stars = 0,
            forkCount = 0,
            issuesResolved = 0,
            isNativeMengine = true
        )
        
        return acquisitionEngine.runCapabilityCompetition("Find better agentic code mod", capabilityName, nativeCandidate)
    }
"""

code = re.sub(r'    suspend fun runCapabilityCompetition[\s\S]*?    }\n', comp_func + "\n", code)

with open("app/src/main/java/com/example/ui/ChatViewModel.kt", "w") as f:
    f.write(code)
