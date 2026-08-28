package com.example.ai.capabilities

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AutonomousWorkerPoolTest {

    private lateinit var workerPool: AutonomousWorkerPool

    @Before
    fun setUp() {
        workerPool = AutonomousWorkerPoolImpl()
    }

    @Test
    fun testWorkerPool_RegistersAll10SpecializedRoles() {
        val workers = workerPool.getWorkers()
        assertEquals(10, workers.size)

        val roles = workers.map { it.role }.toSet()
        assertTrue(roles.contains(WorkerRole.CODER))
        assertTrue(roles.contains(WorkerRole.REPO_ANALYSIS))
        assertTrue(roles.contains(WorkerRole.RESEARCH))
        assertTrue(roles.contains(WorkerRole.BROWSER))
        assertTrue(roles.contains(WorkerRole.TERMINAL))
        assertTrue(roles.contains(WorkerRole.DEVICE))
        assertTrue(roles.contains(WorkerRole.VISUAL_VIDEO))
        assertTrue(roles.contains(WorkerRole.TESTING))
        assertTrue(roles.contains(WorkerRole.SECURITY))
        assertTrue(roles.contains(WorkerRole.DOC_REVIEW))
    }

    @Test
    fun testWorkerSelection_RoutesTaskToOptimalSpecializedWorker() {
        val codingTask = AutonomousWorkerTask("t1", WorkerRole.CODER, "Synthesize Kotlin patch", "fun foo() {}")
        val selectedCoder = workerPool.selectBestWorker(codingTask)
        assertEquals(WorkerRole.CODER, selectedCoder.descriptor.role)

        val secTask = AutonomousWorkerTask("t2", WorkerRole.SECURITY, "Audit diff", "diff --git a b")
        val selectedSec = workerPool.selectBestWorker(secTask)
        assertEquals(WorkerRole.SECURITY, selectedSec.descriptor.role)
    }

    @Test
    fun testTaskExecution_ExecutesAcrossSpecializedWorkers() = runBlocking {
        val secWorker = workerPool.selectBestWorker(
            AutonomousWorkerTask("t-sec", WorkerRole.SECURITY, "Scan clean patch", "val x = 42")
        )
        val secResult = secWorker.executeTask(
            AutonomousWorkerTask("t-sec", WorkerRole.SECURITY, "Scan clean patch", "val x = 42", mapOf("patch" to "val x = 42"))
        )
        assertTrue(secResult.isSuccess)
        assertEquals("worker-security-audit", secResult.workerId)

        val testingWorker = workerPool.selectBestWorker(
            AutonomousWorkerTask("t-test", WorkerRole.TESTING, "Synthesize regression test", "Scenario")
        )
        val testResult = testingWorker.executeTask(
            AutonomousWorkerTask("t-test", WorkerRole.TESTING, "Synthesize regression test", "Scenario")
        )
        assertTrue(testResult.isSuccess)
        assertEquals("worker-testing-regression", testResult.workerId)
        assertNotNull(testResult.artifacts["testFileName"])
    }

    @Test
    fun testParallelTaskExecution() = runBlocking {
        val tasks = listOf(
            AutonomousWorkerTask("p1", WorkerRole.CODER, "Code task", "context"),
            AutonomousWorkerTask("p2", WorkerRole.RESEARCH, "Research task", "context"),
            AutonomousWorkerTask("p3", WorkerRole.DOC_REVIEW, "Doc task", "context")
        )
        val results = workerPool.executeParallelTasks(tasks)
        assertEquals(3, results.size)
        assertTrue(results.all { it.isSuccess })
    }
}
