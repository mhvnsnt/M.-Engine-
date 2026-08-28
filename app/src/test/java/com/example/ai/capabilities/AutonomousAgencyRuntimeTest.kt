package com.example.ai.capabilities

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AutonomousAgencyRuntimeTest {

    private lateinit var agencyLedger: AgencyLedger
    private lateinit var resourceEngine: ResourceGovernanceEngine
    private lateinit var opportunityEngine: OpportunityEngine
    private lateinit var runtime: AutonomousAgencyRuntime

    @Before
    fun setup() {
        agencyLedger = InMemoryAgencyLedger()
        resourceEngine = ResourceGovernanceEngineImpl()
        opportunityEngine = OpportunityEngineImpl()
        val workerPool = AutonomousWorkerPoolImpl()
        val evidenceEngine = EvidenceAssuranceEngineImpl()

        runtime = AutonomousAgencyRuntimeImpl(
            agencyLedger = agencyLedger,
            resourceEngine = resourceEngine,
            opportunityEngine = opportunityEngine,
            workerPool = workerPool,
            evidenceEngine = evidenceEngine
        )
    }

    @Test
    fun testResourceGovernanceEngine_evaluatesConstraints() {
        resourceEngine.setLimits(money = 1.0, timeMs = 10000, tokens = 50000)
        
        val constraintOk = resourceEngine.evaluateAction(0.5, 5000, "LOW")
        assertEquals(ResourceConstraint.OK, constraintOk)

        val constraintMoney = resourceEngine.evaluateAction(1.5, 5000, "LOW")
        assertEquals(ResourceConstraint.MONEY_EXHAUSTED, constraintMoney)

        val constraintTime = resourceEngine.evaluateAction(0.5, 15000, "LOW")
        assertEquals(ResourceConstraint.TIME_EXHAUSTED, constraintTime)

        val constraintRisk = resourceEngine.evaluateAction(0.5, 5000, "CRITICAL")
        assertEquals(ResourceConstraint.RISK_TOO_HIGH, constraintRisk)
    }

    @Test
    fun testAgencyLedger_recordsIntentAndObservations() = runBlocking {
        val entry = AgencyLedgerEntry(
            id = "test-123",
            intent = "Fix NullPointerException in Scanner",
            authorizationStatus = "APPROVED",
            decision = AgencyDecision.PROCEED,
            decisionReasoning = "Matches safety policies",
            actionTaken = "Retrieve logs",
            observation = "Logs retrieved successfully",
            resultStatus = "SUCCESS",
            evidenceId = "ev-1",
            learning = "Logs are in JSON format",
            nextDecisionId = "test-124"
        )

        assertTrue(agencyLedger.recordEntry(entry))
        
        val entries = agencyLedger.getEntriesForIntent("Fix NullPointer")
        assertEquals(1, entries.size)
        assertEquals("test-123", entries.first().id)
    }

    @Test
    fun testOpportunityEngine_scoresAndRanksOpportunities() {
        val opp1 = Opportunity(
            id = "opp-1",
            title = "Automated Security Patching",
            description = "Auto-fix vulnerabilities",
            marketPain = 8.0,
            technicalFeasibility = 7.0,
            differentiation = 9.0,
            distribution = 6.0,
            revenuePotential = 8.0,
            timing = 9.0,
            costRisk = 4.0
        )
        // Score: (8*7*9*6*8*9)/4 = 217728 / 4 = 54432

        val opp2 = Opportunity(
            id = "opp-2",
            title = "Code Formatting",
            description = "Format code on save",
            marketPain = 3.0,
            technicalFeasibility = 9.0,
            differentiation = 2.0,
            distribution = 5.0,
            revenuePotential = 2.0,
            timing = 5.0,
            costRisk = 2.0
        )
        // Score: (3*9*2*5*2*5)/2 = 2700 / 2 = 1350

        val score1 = opportunityEngine.evaluateOpportunity(opp1)
        val score2 = opportunityEngine.evaluateOpportunity(opp2)
        assertTrue(score1 > score2)

        val ranked = opportunityEngine.rankOpportunities(listOf(opp2, opp1))
        assertEquals("opp-1", ranked.first().id)
    }

    @Test
    fun testAutonomousAgencyRuntime_executesMissionWithinResources() = runBlocking {
        resourceEngine.setLimits(money = 100.0, timeMs = 100000, tokens = 1000000)
        
        val context = AgencyContext(
            intent = "Analyze repository for vulnerabilities",
            repositoryTarget = "test/repo",
            initialConstraints = emptyMap()
        )

        val result = runtime.executeMission(context)
        assertTrue(result.isSuccess)
        assertEquals("LEARN", result.stageReached)
        
        val ledgerEntries = agencyLedger.getEntriesForIntent("Analyze repository")
        assertTrue(ledgerEntries.isNotEmpty())
    }

    @Test
    fun testAutonomousAgencyRuntime_haltsOnResourceExhaustion() = runBlocking {
        // Set very low limits to force exhaustion
        resourceEngine.setLimits(money = 0.0, timeMs = 10, tokens = 10)
        
        val context = AgencyContext(
            intent = "Expensive mission",
            repositoryTarget = "test/repo",
            initialConstraints = emptyMap()
        )

        val result = runtime.executeMission(context)
        assertFalse(result.isSuccess)
        assertEquals("INIT", result.stageReached)
        assertTrue(result.message.contains("MONEY_EXHAUSTED") || result.message.contains("TIME_EXHAUSTED"))
        
        val ledgerEntries = agencyLedger.getEntriesForIntent("Expensive mission")
        assertEquals(1, ledgerEntries.size)
        assertEquals(AgencyDecision.HALT_RESOURCE_EXHAUSTED, ledgerEntries.first().decision)
    }

    @Test
    fun testAutonomousAgencyRuntime_haltsOnExternalCapabilityWait() = runBlocking {
        resourceEngine.setLimits(money = 100.0, timeMs = 100000, tokens = 1000000)
        
        val context = AgencyContext(
            intent = "Research capability that requires a provider",
            repositoryTarget = "test/repo",
            initialConstraints = mapOf("mock_provider_fail" to "true")
        )

        val result = runtime.executeMission(context)
        assertFalse(result.isSuccess)
        assertEquals("RESEARCH", result.stageReached)
        assertTrue(result.message.contains("WAITING_FOR_EXTERNAL_CAPABILITY"))
    }
}
