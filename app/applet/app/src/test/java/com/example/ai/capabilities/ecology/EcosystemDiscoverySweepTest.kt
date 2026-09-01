package com.example.ai.capabilities.ecology

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
class EcosystemDiscoverySweepTest {

    @Test
    fun executeRealSweep() {
        val ecologyEngine = ProjectEcologyEngineImpl()
        val githubConnector = GitHubConnector()
        val sweep = EcosystemDiscoverySweep(ecologyEngine, githubConnector)
        
        runBlocking {
            sweep.executeSweep("mhvnsnt")
        }
    }
}
