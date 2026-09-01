package com.example.ai.capabilities.ecology

import com.example.ai.capabilities.federated.GitHubCapability
import com.example.ai.capabilities.federated.GitHubRepoResponse
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
class DeepRepositoryInspectionTest {

    @Test
    fun executeDeepInspection() {
        val ecologyEngine = ProjectEcologyEngineImpl()
        val githubCapability = GitHubCapability()
        val inspectionEngine = DeepRepositoryInspectionEngine(ecologyEngine, githubCapability)
        
        runBlocking {
            // M.-Engine- repo
            inspectionEngine.inspectRepository(
                owner = "mhvnsnt",
                repoName = "M.-Engine-"
            )
            
            // Bannon repo
            inspectionEngine.inspectRepository(
                owner = "mhvnsnt",
                repoName = "Bannon"
            )
        }
    }
}
