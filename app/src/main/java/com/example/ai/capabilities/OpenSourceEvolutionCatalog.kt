package com.example.ai.capabilities

/**
 * Curated open-source capability candidates that can strengthen M. Engine without
 * requiring the native architecture to be discarded.
 *
 * This is intentionally metadata-only: discovery does not grant production trust.
 * AcquisitionEngine must still retrieve, inspect, scan, build, benchmark, and
 * evidence-gate a candidate before integration.
 */
enum class EvolutionFit {
    DURABILITY,
    RESILIENCE,
    OBSERVABILITY,
    CONCURRENCY,
    UI_VERIFICATION,
    STATIC_ANALYSIS,
    WORKFLOW_ORCHESTRATION
}

enum class NonDestructiveMode {
    AUGMENT,
    ADAPTER,
    COMPOSE
}

data class OpenSourceCapabilityCandidate(
    val name: String,
    val repositoryUrl: String,
    val license: String,
    val fits: Set<EvolutionFit>,
    val preferredMode: NonDestructiveMode,
    val rationale: String
)

object OpenSourceEvolutionCatalog {
    /**
     * Candidates are deliberately broad. Version, maintenance, security and
     * compatibility are evaluated at discovery time rather than frozen here.
     */
    val candidates: List<OpenSourceCapabilityCandidate> = listOf(
        OpenSourceCapabilityCandidate(
            name = "kotlinx.coroutines",
            repositoryUrl = "https://github.com/Kotlin/kotlinx.coroutines",
            license = "Apache-2.0",
            fits = setOf(EvolutionFit.CONCURRENCY, EvolutionFit.RESILIENCE),
            preferredMode = NonDestructiveMode.AUGMENT,
            rationale = "Structured concurrency, cancellation and efficient suspension primitives."
        ),
        OpenSourceCapabilityCandidate(
            name = "Resilience4j",
            repositoryUrl = "https://github.com/resilience4j/resilience4j",
            license = "Apache-2.0",
            fits = setOf(EvolutionFit.RESILIENCE),
            preferredMode = NonDestructiveMode.ADAPTER,
            rationale = "Composable retry, circuit-breaker, rate-limit, bulkhead and timeout controls."
        ),
        OpenSourceCapabilityCandidate(
            name = "Temporal Java SDK",
            repositoryUrl = "https://github.com/temporalio/sdk-java",
            license = "Apache-2.0",
            fits = setOf(EvolutionFit.DURABILITY, EvolutionFit.WORKFLOW_ORCHESTRATION),
            preferredMode = NonDestructiveMode.ADAPTER,
            rationale = "Durable workflow primitives suitable for long-lived mission execution and recovery."
        ),
        OpenSourceCapabilityCandidate(
            name = "OpenTelemetry Java",
            repositoryUrl = "https://github.com/open-telemetry/opentelemetry-java",
            license = "Apache-2.0",
            fits = setOf(EvolutionFit.OBSERVABILITY),
            preferredMode = NonDestructiveMode.AUGMENT,
            rationale = "Standardized traces, metrics and logs for the Agency Ledger and Reality Loop."
        ),
        OpenSourceCapabilityCandidate(
            name = "NullAway",
            repositoryUrl = "https://github.com/uber/NullAway",
            license = "MIT",
            fits = setOf(EvolutionFit.STATIC_ANALYSIS),
            preferredMode = NonDestructiveMode.AUGMENT,
            rationale = "Low-overhead nullness analysis that can harden Java-facing boundaries."
        ),
        OpenSourceCapabilityCandidate(
            name = "Showkase",
            repositoryUrl = "https://github.com/airbnb/Showkase",
            license = "Apache-2.0",
            fits = setOf(EvolutionFit.UI_VERIFICATION),
            preferredMode = NonDestructiveMode.AUGMENT,
            rationale = "Composable UI discovery and visualization that can strengthen visual verification."
        )
    )

    fun candidatesFor(fit: EvolutionFit): List<OpenSourceCapabilityCandidate> =
        candidates.filter { fit in it.fits }
}
