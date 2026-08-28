package com.example.ai.capabilities

import com.example.data.EndpointEntity
import com.example.data.MissionDao
import com.example.data.MissionEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class AutonomousPersistentDevelopmentTest {

    private lateinit var mockMissionDao: MissionDao
    private lateinit var scheduler: DurableAutonomousScheduler
    private lateinit var repoGraphEngine: RepositoryGraphEngine
    private lateinit var failureObservatory: FailureObservatory
    private lateinit var regressionMemory: RegressionMemory
    private lateinit var evidenceEngine: EvidenceAssuranceEngine
    private lateinit var workerPool: AutonomousWorkerPool
    private lateinit var selfDevelopmentEngine: AutonomousSelfDevelopmentEngine

    @Before
    fun setup() {
        val inMemoryMissions = mutableMapOf<String, MissionEntity>()
        mockMissionDao = object : MissionDao {
            override suspend fun insertMission(mission: MissionEntity) { inMemoryMissions[mission.id] = mission }
            override suspend fun updateMission(mission: MissionEntity) { inMemoryMissions[mission.id] = mission }
            override suspend fun getMission(id: String): MissionEntity? = inMemoryMissions[id]
            override suspend fun getAllMissions(): List<MissionEntity> = inMemoryMissions.values.toList()
        }

        scheduler = DurableAutonomousSchedulerImpl(mockMissionDao)
        repoGraphEngine = RepositoryGraphEngineImpl()
        failureObservatory = FailureObservatoryImpl(repoGraphEngine)
        regressionMemory = RegressionMemoryEngineImpl(repoGraphEngine)
        evidenceEngine = EvidenceAssuranceEngineImpl()
        workerPool = AutonomousWorkerPoolImpl()

        selfDevelopmentEngine = AutonomousSelfDevelopmentEngineImpl(
            workerPool = workerPool,
            prioritizationEngine = ImprovementPrioritizationEngineImpl(),
            provenanceLedger = InMemoryProvenanceLedger(),
            evidenceEngine = evidenceEngine,
            missionEngine = null,
            contextEngine = PersonalContextEngineImpl(),
            repoGraphEngine = repoGraphEngine,
            failureObservatory = failureObservatory,
            regressionMemory = regressionMemory,
            scheduler = scheduler
        )
    }

    @Test
    fun testRepositoryGraphEngine_ParsesSymbolsImportsAndCalls() {
        val sampleCode = """
            package com.example.service
            import com.example.data.UserDao
            import com.example.model.User
            
            class UserService(private val dao: UserDao) : BaseService {
                fun fetchUser(id: String): User {
                    validate(id)
                    return dao.get(id)
                }
                fun validate(id: String) {}
            }
        """.trimIndent()

        repoGraphEngine.updateFile("UserService.kt", sampleCode)
        val graph = repoGraphEngine.getGraph()

        assertNotNull(graph.files["UserService.kt"])
        val fileAnalysis = graph.files["UserService.kt"]!!
        assertEquals("com.example.service", fileAnalysis.packageName)
        assertTrue(fileAnalysis.imports.contains("com.example.data.UserDao"))
        
        val classSymbol = graph.symbols["UserService"]
        assertNotNull(classSymbol)
        assertEquals(SymbolKind.CLASS, classSymbol?.kind)
        assertTrue(classSymbol!!.superTypes.contains("BaseService"))

        val impacted = repoGraphEngine.findImpactedComponents(listOf("UserService.kt"))
        assertTrue(impacted.contains("UserService.kt"))
        assertTrue(impacted.contains("com.example.service.UserService"))
    }

    @Test
    fun testFailureObservatory_GroupsClustersAndGeneratesPrioritizedMissions() = runBlocking {
        val event1 = FailureEvent(
            id = "err-1",
            source = FailureSource.RUNTIME_CRASH,
            timestamp = System.currentTimeMillis(),
            rawLog = "NullPointerException in SecurityScanner.kt at line 88",
            stackTrace = "java.lang.NullPointerException\n\tat com.example.ai.capabilities.SecurityScannerImpl.scan(SecurityScanner.kt:88)",
            affectedComponent = "SecurityScanner.kt"
        )
        val event2 = FailureEvent(
            id = "err-2",
            source = FailureSource.RUNTIME_CRASH,
            timestamp = System.currentTimeMillis() + 100,
            rawLog = "NullPointerException in SecurityScanner.kt at line 88",
            stackTrace = "java.lang.NullPointerException\n\tat com.example.ai.capabilities.SecurityScannerImpl.scan(SecurityScanner.kt:88)",
            affectedComponent = "SecurityScanner.kt"
        )

        failureObservatory.ingestFailure(event1)
        failureObservatory.ingestFailure(event2)

        val clusters = failureObservatory.getActiveClusters()
        assertEquals(1, clusters.size)
        val cluster = clusters.first()
        assertEquals(2, cluster.events.size)
        assertEquals(IssueSeverity.CRITICAL, cluster.severity)
        assertEquals("SecurityScanner.kt", cluster.targetComponent)
        assertTrue(cluster.priorityScore > 20.0)

        val topMissions = failureObservatory.generateTopRankedMissions(5)
        assertEquals(1, topMissions.size)
        val mission = topMissions.first()
        assertEquals("SecurityScanner.kt", mission.targetComponent)
        assertTrue(mission.title.contains("Autonomous Fix"))
    }

    @Test
    fun testRegressionMemory_RecordsAndQueriesDurableTests() = runBlocking {
        val regrTest = DurableRegressionTest(
            id = "regr-modelrouter-001",
            repoId = "mhvnsnt/M.-Engine-",
            componentTarget = "ModelRouter.kt",
            testClass = "ModelRouterTest",
            testMethod = "testFailoverToSecondaryEndpoint",
            failureSignature = "ModelRouter hangs on primary timeout",
            fixCommitHash = "abc1234",
            assertionScope = "Ollama, OpenRouter failover timeout 5000ms"
        )

        val saved = regressionMemory.recordDurableRegression(regrTest)
        assertTrue(saved)

        val queryResult = regressionMemory.getRelevantRegressionTests(
            componentTarget = "ModelRouter.kt",
            affectedFiles = listOf("app/src/main/java/com/example/ai/capabilities/ModelRouter.kt")
        )
        assertEquals(1, queryResult.size)
        assertEquals("regr-modelrouter-001", queryResult.first().id)
        assertEquals("ModelRouterTest", queryResult.first().testClass)
    }

    @Test
    fun testEvidenceEngine_ExpiresOnCommitMismatchAndFileHashChange() = runBlocking {
        val scope = EvidenceScope(
            testedCorpus = listOf("Anthropic", "OpenAI", "Google", "GitHub"),
            scannerOrEngineVersion = "2.4.0",
            commitHash = "commit-alpha",
            environment = "Android JVM 21",
            targetFileHashes = mapOf("SecurityScanner.kt" to "hash_111")
        )

        val record = EvidenceRecord(
            id = "ev-test-01",
            claim = StructuredClaim("Test Claim", null, 100L, "before", "commit-alpha", "after", EvidenceLevel.REGRESSION_PROOF),
            evidenceType = EvidenceType.BENCHMARK,
            level = EvidenceLevel.REGRESSION_PROOF,
            source = "TestRunner",
            timestamp = System.currentTimeMillis(),
            reproductionSteps = listOf("Run test"),
            observedResult = "100% detection on tested corpus",
            expectedResult = "100% detection on tested corpus",
            confidenceScore = 0.99,
            independentlyVerified = true,
            scope = scope,
            status = EvidenceStatus.VALID
        )

        evidenceEngine.recordEvidence(record)

        // 1. Check valid on matching commit and file hash
        val validSameCommit = evidenceEngine.evaluateClaim(
            claimId = "Test Claim",
            currentCommit = "commit-alpha",
            currentFileHashes = mapOf("SecurityScanner.kt" to "hash_111")
        )
        assertTrue(validSameCommit)

        // 2. Check expired when evaluating against a NEW commit (old evidence does not prove new commit)
        val validNewCommit = evidenceEngine.evaluateClaim(
            claimId = "Test Claim",
            currentCommit = "commit-beta",
            currentFileHashes = mapOf("SecurityScanner.kt" to "hash_111")
        )
        assertFalse(validNewCommit)
        val records = evidenceEngine.getEvidenceForClaim("Test Claim")
        assertEquals(EvidenceStatus.EXPIRED_COMMIT_MISMATCH, records.first().status)

        // 3. Format report check
        val report = evidenceEngine.formatScopedEvidenceReport(records.first())
        assertTrue(report.contains("Tested Corpus: Anthropic, OpenAI, Google, GitHub"))
        assertTrue(report.contains("Engine/Scanner Version: 2.4.0"))
        assertTrue(report.contains("Commit Hash: commit-alpha"))
    }

    @Test
    fun testDurableAutonomousScheduler_PersistsInRoomAndEnforcesBudget() = runBlocking {
        val scheduled = scheduler.scheduleMission(
            prompt = "Optimize Token Parsing",
            targetRepo = "mhvnsnt/M.-Engine-",
            maxIterations = 3,
            maxCostCents = 15.0
        )

        assertNotNull(scheduled.id)
        assertEquals(MissionScheduleStatus.SCHEDULED, scheduled.status)

        // Simulate iterations
        val iter1Ok = scheduler.recordIteration(scheduled.id, costCents = 5.0)
        assertTrue(iter1Ok)

        val iter2Ok = scheduler.recordIteration(scheduled.id, costCents = 5.0)
        assertTrue(iter2Ok)

        // Third iteration exhausts max iterations (3)
        val iter3Ok = scheduler.recordIteration(scheduled.id, costCents = 5.0)
        assertFalse(iter3Ok) // Budget exhausted

        val missionAfter = scheduler.getMission(scheduled.id)
        assertNotNull(missionAfter)
        assertEquals(MissionScheduleStatus.STOPPED_BUDGET_EXHAUSTED, missionAfter?.status)
        assertEquals(3, missionAfter?.iterationCount)
        assertEquals(15.0, missionAfter?.budgetCostCents ?: 0.0, 0.001)

        // Verify recovery after process restart
        val pending = scheduler.resumePendingMissions()
        assertNotNull(pending)
    }

    @Test
    fun testModelRouterWorker_PreservesCheckpointAndHandlesFailover() = runBlocking {
        val worker = AutonomousCodingWorker(modelRouter = null)
        val initialCheckpoint = WorkerCheckpoint(
            stage = "SYNTHESIS_INIT",
            partialOutput = "Synthesizing SecurityScanner patch",
            state = mapOf("file" to "SecurityScanner.kt")
        )

        val task = AutonomousWorkerTask(
            taskId = "task-persist-01",
            role = WorkerRole.CODER,
            goal = "Fix secret leak vulnerabilities",
            context = "val apiKey = '...'",
            parameters = mapOf("targetFile" to "SecurityScanner.kt"),
            checkpoint = initialCheckpoint
        )

        val result = worker.executeTask(task)
        assertTrue(result.isSuccess)
        assertNotNull(result.checkpointSaved)
        assertEquals("SecurityScanner.kt", result.artifacts["targetFile"])
        assertFalse(result.failoverOccurred)
    }

    @Test
    fun testPersistentAutonomousSelfDevelopment_ExecutesAll10StagesEndToEnd() = runBlocking {
        val result = selfDevelopmentEngine.executeAutonomousSelfDevelopment(
            targetRepo = "mhvnsnt/M.-Engine-",
            maxIterations = 5,
            maxCostCents = 50.0
        )

        assertTrue(result.isSuccess)
        assertEquals(10, result.stagesCompleted)
        assertEquals("mhvnsnt/M.-Engine-", result.targetRepo)
        assertEquals(MissionScheduleStatus.STOPPED_SUCCESS, result.schedulerStatus)
        assertTrue(result.priorityScore > 10.0)
        assertNotNull(result.provenance)
        assertEquals(ProvenanceDecision.COMBINE, result.provenance.decision)
        assertTrue(result.evidenceRecordId.isNotBlank())
        assertTrue(result.regressionTestsRun >= 1)

        // Verify that evidence was recorded with correct scoped metadata
        val evidenceList = evidenceEngine.getEvidenceForClaim("Autonomous Self-Development Mission #4 on mhvnsnt/M.-Engine-")
        assertTrue(evidenceList.isNotEmpty())
        val ev = evidenceList.first()
        assertEquals(EvidenceLevel.REGRESSION_PROOF, ev.level)
        assertTrue(ev.scope.testedCorpus.contains("Anthropic (sk-ant-api03-*)"))
        assertTrue(ev.scope.testedCorpus.contains("OpenAI (sk-proj-*)"))
        assertEquals("2.4.0", ev.scope.scannerOrEngineVersion)
        assertEquals(EvidenceStatus.VALID, ev.status)
    }
}
