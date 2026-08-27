package com.example.ai.capabilities

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase12IntegrationTest {

    @Test
    fun testRecursiveAuditAndDuplicateDetection() = runBlocking {
        val graph = CapabilityGraphDatabaseImpl()
        val auditor = RecursiveAuditorImpl()
        
        auditor.auditRepository("/simulated/path", graph)
        
        val nodes = graph.getNodesByDomain(CapabilityDomain.CODING)
        assertEquals(1, nodes.size) // We added the same capability twice (duplicates)
        
        val duplicates = graph.findDuplicates()
        assertEquals(1, duplicates.size)
        assertEquals(2, duplicates.first().implementations.size)
    }
    
    @Test
    fun testSelfImprovementExperiment() = runBlocking {
        val graph = CapabilityGraphDatabaseImpl()
        val auditor = RecursiveAuditorImpl()
        auditor.auditRepository("/simulated/path", graph)
        
        val experiment = SelfImprovementExperimentImpl(
            EvidenceAssuranceEngineImpl(),
            Phase9IntegrationTest.MockGitHubService(),
            Phase10IntegrationTest.MockCiCd()
        )
        
        val result = experiment.runExperiment(graph)
        assertTrue(result) // The evidence level was INDEPENDENT_VERIFICATION which passes Reality Check
    }
}
