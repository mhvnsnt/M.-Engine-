package com.example.ai.capabilities.ecology

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

data class BudgetReservation(
    val reservationId: String = UUID.randomUUID().toString().take(8),
    val workerId: String,
    val reservedActions: Int = 1,
    val reservedNetworkCalls: Int = 1,
    val reservedModelCalls: Int = 0,
    val reservedCostUsd: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

class AtomicBudgetGovernor(
    val initialBudget: ExecutionBudget
) {
    private val mutex = Mutex()
    private val activeReservations = mutableMapOf<String, BudgetReservation>()
    
    var consumedActions: Int = 0
        private set
    var consumedNetworkCalls: Int = 0
        private set
    var consumedModelCalls: Int = 0
        private set
    var consumedCostUsd: Double = 0.0
        private set

    suspend fun tryReserve(
        workerId: String,
        actions: Int = 1,
        networkCalls: Int = 1,
        modelCalls: Int = 0,
        costUsd: Double = 0.0
    ): BudgetReservation? = mutex.withLock {
        // Check time elapsed
        val timeElapsed = System.currentTimeMillis() - initialBudget.startTimeMs
        if (timeElapsed > initialBudget.maxExecutionTimeMs) {
            return null
        }

        // Sum reserved resources currently in-flight
        val inFlightActions = activeReservations.values.sumOf { it.reservedActions }
        val inFlightNetwork = activeReservations.values.sumOf { it.reservedNetworkCalls }
        val inFlightModel = activeReservations.values.sumOf { it.reservedModelCalls }
        val inFlightCost = activeReservations.values.sumOf { it.reservedCostUsd }

        val committedActions = consumedActions + inFlightActions
        val committedNetwork = consumedNetworkCalls + inFlightNetwork
        val committedModel = consumedModelCalls + inFlightModel
        val committedCost = consumedCostUsd + inFlightCost

        // Verify bounds
        if (committedActions + actions > initialBudget.maxIterations) return null
        if (committedNetwork + networkCalls > initialBudget.maxNetworkCalls) return null
        if (committedModel + modelCalls > initialBudget.maxHighCostModelCalls) return null
        if (committedCost + costUsd > initialBudget.maxCostUsd) return null

        val reservation = BudgetReservation(
            workerId = workerId,
            reservedActions = actions,
            reservedNetworkCalls = networkCalls,
            reservedModelCalls = modelCalls,
            reservedCostUsd = costUsd
        )
        activeReservations[reservation.reservationId] = reservation
        return reservation
    }

    suspend fun reconcile(
        reservation: BudgetReservation,
        actualCost: CostMetrics,
        actualActions: Int = 1
    ) = mutex.withLock {
        activeReservations.remove(reservation.reservationId)
        consumedActions += actualActions
        consumedNetworkCalls += actualCost.networkCalls
        consumedModelCalls += actualCost.modelCalls
        consumedCostUsd += actualCost.costUsd
    }

    suspend fun cancelReservation(reservationId: String) = mutex.withLock {
        activeReservations.remove(reservationId)
    }

    suspend fun getRemainingBudget(): ExecutionBudget = mutex.withLock {
        val inFlightActions = activeReservations.values.sumOf { it.reservedActions }
        val inFlightNetwork = activeReservations.values.sumOf { it.reservedNetworkCalls }
        val inFlightModel = activeReservations.values.sumOf { it.reservedModelCalls }
        val inFlightCost = activeReservations.values.sumOf { it.reservedCostUsd }

        val remainingActions = (initialBudget.maxIterations - (consumedActions + inFlightActions)).coerceAtLeast(0)
        val remainingNetwork = (initialBudget.maxNetworkCalls - (consumedNetworkCalls + inFlightNetwork)).coerceAtLeast(0)
        val remainingModel = (initialBudget.maxHighCostModelCalls - (consumedModelCalls + inFlightModel)).coerceAtLeast(0)
        val remainingCost = (initialBudget.maxCostUsd - (consumedCostUsd + inFlightCost)).coerceAtLeast(0.0)

        initialBudget.copy(
            maxIterations = remainingActions,
            maxNetworkCalls = remainingNetwork,
            maxHighCostModelCalls = remainingModel,
            maxCostUsd = remainingCost
        )
    }

    suspend fun getConsumedMetrics(): CostMetrics = mutex.withLock {
        CostMetrics(
            networkCalls = consumedNetworkCalls,
            modelCalls = consumedModelCalls,
            costUsd = consumedCostUsd
        )
    }

    suspend fun isExhausted(): Boolean = mutex.withLock {
        val timeElapsed = System.currentTimeMillis() - initialBudget.startTimeMs
        if (timeElapsed > initialBudget.maxExecutionTimeMs) return true
        
        val inFlightActions = activeReservations.values.sumOf { it.reservedActions }
        val committedActions = consumedActions + inFlightActions
        if (committedActions >= initialBudget.maxIterations) return true

        val inFlightCost = activeReservations.values.sumOf { it.reservedCostUsd }
        val committedCost = consumedCostUsd + inFlightCost
        if (committedCost >= initialBudget.maxCostUsd) return true

        return false
    }
}
