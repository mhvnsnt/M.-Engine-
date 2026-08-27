package com.example.ai.capabilities

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
}
