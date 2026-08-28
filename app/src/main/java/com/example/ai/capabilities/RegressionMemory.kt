package com.example.ai.capabilities

import java.util.concurrent.ConcurrentHashMap

data class DurableRegressionTest(
    val id: String,
    val repoId: String,
    val componentTarget: String,
    val testClass: String,
    val testMethod: String,
    val failureSignature: String,
    val fixCommitHash: String,
    val assertionScope: String,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val status: String = "ACTIVE"
)

data class RegressionScenario(
    val id: String,
    val description: String,
    val testScenario: TestScenario,
    val expectedOutcome: String
)

interface RegressionMemory {
    suspend fun getKnownBugs(repo: RepositoryRef): List<String>
    suspend fun getFixedBugs(repo: RepositoryRef): List<String>
    suspend fun getRegressionTests(repo: RepositoryRef): List<RegressionScenario>
    suspend fun getKnownFragileSystems(repo: RepositoryRef): List<String>
    suspend fun saveRegressionTest(repo: RepositoryRef, scenario: RegressionScenario): Boolean
    
    // Durable Regression Engine APIs
    suspend fun recordDurableRegression(test: DurableRegressionTest): Boolean
    suspend fun getRelevantRegressionTests(componentTarget: String, affectedFiles: List<String> = emptyList()): List<DurableRegressionTest>
    suspend fun getAllRegressionTests(): List<DurableRegressionTest>
}

class RegressionMemoryEngineImpl(
    private val repoGraphEngine: RepositoryGraphEngine? = null
) : RegressionMemory {
    private val knownBugsMap = ConcurrentHashMap<String, MutableList<String>>()
    private val fixedBugsMap = ConcurrentHashMap<String, MutableList<String>>()
    private val fragileSystemsMap = ConcurrentHashMap<String, MutableList<String>>()
    private val scenariosMap = ConcurrentHashMap<String, MutableList<RegressionScenario>>()
    private val durableTests = ConcurrentHashMap<String, DurableRegressionTest>()

    init {
        // Seed baseline historical regressions
        val baselineSecurity = DurableRegressionTest(
            id = "regr-sec-001",
            repoId = "mhvnsnt/M.-Engine-",
            componentTarget = "SecurityScanner.kt",
            testClass = "SecurityScannerRegressionTest",
            testMethod = "testSecurityScannerCatchesAllApiKeys",
            failureSignature = "Hardcoded API keys pass security scanner",
            fixCommitHash = "683e831",
            assertionScope = "Anthropic, OpenAI, Google, GitHub, AWS, Slack API Key Regex Signatures"
        )
        durableTests[baselineSecurity.id] = baselineSecurity
    }

    override suspend fun getKnownBugs(repo: RepositoryRef): List<String> {
        return knownBugsMap[repo.name] ?: emptyList()
    }

    override suspend fun getFixedBugs(repo: RepositoryRef): List<String> {
        return fixedBugsMap[repo.name] ?: emptyList()
    }

    override suspend fun getRegressionTests(repo: RepositoryRef): List<RegressionScenario> {
        return scenariosMap[repo.name] ?: emptyList()
    }

    override suspend fun getKnownFragileSystems(repo: RepositoryRef): List<String> {
        return fragileSystemsMap[repo.name] ?: listOf("SecurityScanner.kt", "ModelRouter.kt", "CiCdPipeline.kt")
    }

    override suspend fun saveRegressionTest(repo: RepositoryRef, scenario: RegressionScenario): Boolean {
        scenariosMap.getOrPut(repo.name) { mutableListOf() }.add(scenario)
        return true
    }

    override suspend fun recordDurableRegression(test: DurableRegressionTest): Boolean {
        durableTests[test.id] = test
        fixedBugsMap.getOrPut(test.repoId) { mutableListOf() }.add(test.failureSignature)
        return true
    }

    override suspend fun getRelevantRegressionTests(
        componentTarget: String,
        affectedFiles: List<String>
    ): List<DurableRegressionTest> {
        val targetName = componentTarget.substringAfterLast("/")
        return durableTests.values.filter { regr ->
            regr.componentTarget.contains(targetName) ||
            regr.componentTarget == componentTarget ||
            affectedFiles.any { it.contains(regr.componentTarget) || regr.componentTarget.contains(it.substringAfterLast("/")) }
        }
    }

    override suspend fun getAllRegressionTests(): List<DurableRegressionTest> {
        return durableTests.values.toList()
    }
}

