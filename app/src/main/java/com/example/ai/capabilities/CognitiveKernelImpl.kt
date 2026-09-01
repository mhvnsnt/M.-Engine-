package com.example.ai.capabilities

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger

class InvalidCognitiveTransitionException(from: CognitiveState, to: CognitiveState) : 
    IllegalStateException("Invalid state transition from $from to $to")

class CognitiveKernelImpl(
    private val jobStateRepository: JobStateRepository,
    private val jobId: String,
    initialState: CognitiveState = CognitiveState.QUEUED
) : CognitiveKernel {

    private val _currentStateFlow = MutableStateFlow(initialState)
    val stateFlow: StateFlow<CognitiveState> = _currentStateFlow.asStateFlow()

    override val currentState: CognitiveState
        get() = _currentStateFlow.value

    private val validTransitions = mapOf(
        CognitiveState.QUEUED to setOf(CognitiveState.UNDERSTAND, CognitiveState.CANCELLING, CognitiveState.CANCELLED, CognitiveState.FAILED),
        CognitiveState.UNDERSTAND to setOf(CognitiveState.RESEARCH, CognitiveState.RETRIEVE, CognitiveState.PLAN, CognitiveState.FAILED, CognitiveState.CANCELLING),
        CognitiveState.RESEARCH to setOf(CognitiveState.RETRIEVE, CognitiveState.PLAN, CognitiveState.FAILED, CognitiveState.CANCELLING),
        CognitiveState.RETRIEVE to setOf(CognitiveState.PLAN, CognitiveState.FAILED, CognitiveState.CANCELLING),
        CognitiveState.PLAN to setOf(CognitiveState.RISK_EVALUATION, CognitiveState.FAILED, CognitiveState.CANCELLING),
        CognitiveState.RISK_EVALUATION to setOf(CognitiveState.WAITING_APPROVAL, CognitiveState.DELEGATE, CognitiveState.FAILED, CognitiveState.CANCELLING),
        CognitiveState.WAITING_APPROVAL to setOf(CognitiveState.DELEGATE, CognitiveState.CANCELLED, CognitiveState.FAILED, CognitiveState.CANCELLING),
        CognitiveState.DELEGATE to setOf(CognitiveState.SANDBOX_CREATING, CognitiveState.ACQUIRING_CAPABILITY, CognitiveState.FAILED, CognitiveState.CANCELLING),
        CognitiveState.ACQUIRING_CAPABILITY to setOf(CognitiveState.SANDBOX_CREATING, CognitiveState.FAILED, CognitiveState.CANCELLING),
        CognitiveState.SANDBOX_CREATING to setOf(CognitiveState.REPOSITORY_LOADING, CognitiveState.FAILED, CognitiveState.CANCELLING),
        CognitiveState.REPOSITORY_LOADING to setOf(CognitiveState.WORKER_STARTING, CognitiveState.FAILED, CognitiveState.CANCELLING),
        CognitiveState.WORKER_STARTING to setOf(CognitiveState.EXECUTING, CognitiveState.FAILED, CognitiveState.CANCELLING),
        CognitiveState.EXECUTING to setOf(CognitiveState.BUILDING, CognitiveState.TESTING, CognitiveState.INSPECTING, CognitiveState.VERIFYING, CognitiveState.FAILED, CognitiveState.CANCELLING),
        CognitiveState.BUILDING to setOf(CognitiveState.TESTING, CognitiveState.INSPECTING, CognitiveState.VERIFYING, CognitiveState.FAILED, CognitiveState.CANCELLING, CognitiveState.REFLECTING),
        CognitiveState.TESTING to setOf(CognitiveState.INSPECTING, CognitiveState.VERIFYING, CognitiveState.FAILED, CognitiveState.CANCELLING, CognitiveState.REFLECTING),
        CognitiveState.INSPECTING to setOf(CognitiveState.VERIFYING, CognitiveState.FAILED, CognitiveState.CANCELLING, CognitiveState.REFLECTING),
        CognitiveState.VERIFYING to setOf(CognitiveState.COMPLETED, CognitiveState.REFLECTING, CognitiveState.FAILED, CognitiveState.CANCELLING),
        CognitiveState.REFLECTING to setOf(CognitiveState.ADAPTING, CognitiveState.FAILED, CognitiveState.CANCELLING),
        CognitiveState.ADAPTING to setOf(CognitiveState.PLAN, CognitiveState.EXECUTING, CognitiveState.FAILED, CognitiveState.CANCELLING),
        CognitiveState.FAILED to setOf(),
        CognitiveState.COMPLETED to setOf(),
        CognitiveState.CANCELLING to setOf(CognitiveState.CANCELLED, CognitiveState.FAILED),
        CognitiveState.CANCELLED to setOf()
    )

    private val iterationCount = AtomicInteger(0)
    private val MAX_ITERATIONS = 5

    override suspend fun transitionTo(state: CognitiveState) {
        val current = _currentStateFlow.value

        // Validate transition
        if (state !in validTransitions[current].orEmpty()) {
            throw InvalidCognitiveTransitionException(current, state)
        }

        // Cycle counting for adaptation loops
        if (state == CognitiveState.ADAPTING) {
            val count = iterationCount.incrementAndGet()
            if (count > MAX_ITERATIONS) {
                // Force a failure if we exceed retry bounds
                println("CognitiveKernel: Max iterations exceeded. Forcing FAILED state.")
                forceTransitionTo(CognitiveState.FAILED, "Max adaptation iterations exceeded.")
                return
            }
        }

        // Perform transition
        _currentStateFlow.value = state
        
        // Persist durably
        jobStateRepository.updateJobState(jobId, state.name)
    }

    private suspend fun forceTransitionTo(state: CognitiveState, reason: String) {
        _currentStateFlow.value = state
        jobStateRepository.updateJobState(jobId, state.name, reason)
    }

    suspend fun cancelJob(reason: String = "User requested cancellation") {
        val current = _currentStateFlow.value
        if (current == CognitiveState.COMPLETED || current == CognitiveState.FAILED || current == CognitiveState.CANCELLED) {
            return // Already terminal
        }
        forceTransitionTo(CognitiveState.CANCELLING, reason)
        // Simulate waiting for remote worker cleanup, then transition to CANCELLED
        forceTransitionTo(CognitiveState.CANCELLED, "Cancellation complete")
    }
}
