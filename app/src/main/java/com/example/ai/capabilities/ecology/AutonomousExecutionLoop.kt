package com.example.ai.capabilities.ecology

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class LoopResult(
    val iterationsCompleted: Int,
    val capabilityResults: List<CapabilityResult>,
    val reasonForExit: String,
    val contradictionsFound: List<Contradiction> = emptyList(),
    val queuedOpportunities: List<String> = emptyList(),
    val cycleState: AutonomousCycleState? = null
)

class AutonomousExecutionLoop(
    val budget: ExecutionBudget,
    private val capabilities: List<AgencyCapability>,
    private val reconciliationEngine: EvidenceReconciliationEngine = EvidenceReconciliationEngine(),
    val cycleId: String = UUID.randomUUID().toString().take(8),
    val objective: String = "Execute bounded ecosystem observation and candidate patch synthesis"
) {
    private val TAG = "AutonomousExecutionLoop"
    private val budgetGovernor = AtomicBudgetGovernor(budget)
    private val cancelledWorkers = ConcurrentHashMap.newKeySet<String>()
    private var isCycleCancelled = false

    var currentCycleState: AutonomousCycleState = AutonomousCycleState(
        cycleId = cycleId,
        objective = objective,
        initialBudget = budget,
        budgetRemaining = budget.copy()
    )
        private set

    fun cancelCycle(reason: String = "MANUALLY_CANCELLED_BY_OWNER") {
        isCycleCancelled = true
        currentCycleState.status = "CANCELLED"
        currentCycleState.exitReason = reason
        println("GOVERNANCE: Autonomous Cycle $cycleId cancelled. Reason: $reason")
    }

    fun cancelWorker(workerId: String) {
        cancelledWorkers.add(workerId)
        val job = currentCycleState.workerJobs.find { it.workerId == workerId }
        if (job != null) {
            job.state = WorkerJobState.CANCELLED
            job.failureReason = "Worker cancelled by owner control plane"
            job.completedAt = System.currentTimeMillis()
        }
        println("GOVERNANCE: Worker $workerId cancelled.")
    }

    suspend fun run(wakeRecord: MetabolismWakeRecord): LoopResult {
        println("━━━━━━━━ AUTONOMOUS EXECUTION LOOP STARTED: $cycleId ━━━━━━━━")
        var iterations = 0
        val allResults = mutableListOf<CapabilityResult>()
        val allContradictions = mutableListOf<Contradiction>()
        val queuedOpportunities = mutableListOf<String>()
        var exitReason = "UNKNOWN"

        while (true) {
            if (isCycleCancelled) {
                exitReason = "CANCELLED"
                break
            }

            if (budgetGovernor.isExhausted()) {
                exitReason = "BUDGET_EXHAUSTED"
                currentCycleState.status = "BUDGET_EXHAUSTED"
                println("BLOCKED: Resource budget exhausted.")
                break
            }

            if (!AutonomyControlPlane.isExecutionAllowed()) {
                exitReason = "AUTONOMY_PAUSED_OR_STOPPED"
                currentCycleState.status = "PAUSED"
                println("BLOCKED: Autonomy is not allowed (Paused or Emergency Stop).")
                break
            }

            // 1. Observe & Update Evidence (Check if priority work exists)
            var priorityWorkExists = checkPriorityWork(iterations)
            if (!priorityWorkExists) {
                val opportunities = if (iterations < 2) OpportunityDiscoveryEngine.discoverOpportunities() else emptyList()
                if (opportunities.isNotEmpty()) {
                    val topOpp = opportunities.first()
                    priorityWorkExists = true
                } else {
                    exitReason = "NO_OPPORTUNITIES_FOUND"
                    break
                }
            }

            // 2. Select Relevant Capabilities
            var available = capabilities.filter { it.isAvailable() }
            if (available.isEmpty()) {
                // Autonomous trigger: perform reality sweep to physically probe unverified capabilities
                CapabilityRealitySweepEngine.executeSweep()
                available = capabilities.filter { it.isAvailable() }
                if (available.isEmpty()) {
                    exitReason = "NO_CAPABILITIES_AVAILABLE"
                    break
                }
            }

            // Select up to maxParallelWorkers bounded by budget
            val workersToSpawn = if (available.size > 1 && budget.maxParallelWorkers > 1) {
                available.take(budget.maxParallelWorkers)
            } else {
                listOf(available.first())
            }

            // 3. Delegate in Parallel with Atomic Reservations
            val iterationResults: List<CapabilityResult> = coroutineScope {
                workersToSpawn.mapIndexed { index, worker ->
                    val job = WorkerJob(
                        parentCycleId = cycleId,
                        capabilityId = worker.capabilityId,
                        objective = "Execute ${worker.name} for iteration $iterations",
                        state = WorkerJobState.QUEUED
                    )
                    currentCycleState.workerJobs.add(job)

                    async {
                        if (cancelledWorkers.contains(job.workerId) || isCycleCancelled) {
                            job.state = WorkerJobState.CANCELLED
                            job.completedAt = System.currentTimeMillis()
                            return@async null
                        }

                        // Atomic Budget Reservation Check (only first worker accounts for the iteration step)
                        val reservation = budgetGovernor.tryReserve(
                            workerId = job.workerId,
                            actions = if (index == 0) 1 else 0,
                            networkCalls = 1,
                            modelCalls = 0,
                            costUsd = 0.005
                        )

                        if (reservation == null) {
                            job.state = WorkerJobState.BUDGET_EXHAUSTED
                            job.failureReason = "Atomic budget reservation failed: Quota exhausted"
                            job.completedAt = System.currentTimeMillis()
                            return@async null
                        }

                        job.state = WorkerJobState.EXECUTING
                        try {
                            val result = worker.execute(mapOf("iteration" to iterations, "timestamp" to System.currentTimeMillis()))
                            
                            if (cancelledWorkers.contains(job.workerId) || isCycleCancelled) {
                                budgetGovernor.cancelReservation(reservation.reservationId)
                                job.state = WorkerJobState.CANCELLED
                                job.completedAt = System.currentTimeMillis()
                                return@async null
                            }

                            // Reconcile actual consumption atomically
                            budgetGovernor.reconcile(reservation, result.costMetrics, actualActions = if (index == 0) 1 else 0)

                            job.state = if (result.success) WorkerJobState.SUCCEEDED else WorkerJobState.FAILED
                            job.completedAt = System.currentTimeMillis()
                            job.costConsumed = result.costMetrics
                            job.evidenceProduced = result.evidence
                            job.resultClassification = if (result.success) "VERIFIED" else "FAILED"
                            return@async result
                        } catch (e: Exception) {
                            budgetGovernor.cancelReservation(reservation.reservationId)
                            job.state = WorkerJobState.FAILED
                            job.failureReason = e.message ?: "Unknown worker failure"
                            job.completedAt = System.currentTimeMillis()
                            return@async null
                        }
                    }
                }.awaitAll().filterNotNull()
            }

            allResults.addAll(iterationResults)

            // Update remaining state
            currentCycleState.budgetConsumed = budgetGovernor.getConsumedMetrics()
            currentCycleState.budgetRemaining = budgetGovernor.getRemainingBudget()

            // 4. Merge Evidence, Reconcile Contradictions & Revision
            for (res in iterationResults) {
                queuedOpportunities.addAll(res.nextPossibilities)

                val rel = DependencyRelationship(
                    sourceId = res.authorizationUsed ?: "CapabilityExecution",
                    targetId = "ProjectEcology",
                    relationshipType = ProjectRelationship.DEPENDS_ON,
                    epistemicClassification = EpistemicClassification.OBSERVATION,
                    confidence = 0.9,
                    evidence = res.evidence,
                    verificationMethod = "DIRECT_EXECUTION",
                    falsificationCondition = null,
                    status = EdgeStatus.ACTIVE
                )
                val contradiction = reconciliationEngine.reconcile(
                    relationship = rel,
                    documentaryEvidence = res.observations,
                    structuralEvidence = res.evidence
                )
                if (contradiction != null) {
                    allContradictions.add(contradiction)
                    reconciliationEngine.printContradiction(contradiction)
                }
            }

            iterations++
        }

        if (exitReason == "UNKNOWN") {
            exitReason = "COMPLETED"
            currentCycleState.status = "COMPLETED"
        }
        currentCycleState.exitReason = exitReason

        println("━━━━━━━━ AUTONOMOUS EXECUTION LOOP ENDED: $cycleId ━━━━━━━━")
        println("SUMMARY: Completed $iterations iterations, gathered ${allResults.size} capability results. Exit: $exitReason")

        return LoopResult(
            iterationsCompleted = iterations,
            capabilityResults = allResults,
            reasonForExit = exitReason,
            contradictionsFound = allContradictions,
            queuedOpportunities = queuedOpportunities.distinct(),
            cycleState = currentCycleState
        )
    }

    private fun checkPriorityWork(currentIteration: Int): Boolean {
        return currentIteration < 3
    }
}
