# M. Engine — Non-Destructive Open-Source Evolution

## Directive

M. Engine evolves by **addition, adaptation, composition and optional augmentation**.
It does not autonomously delete or replace a working native capability merely because an external project appears more mature.

The native implementation remains the reference path until an explicitly authorized migration changes that rule.

## Evidence-gated lifecycle

For every open-source candidate:

1. Discover the current project and relevant historical versions.
2. Verify repository identity, license and provenance.
3. Inspect dependency graph and transitive risk.
4. Check maintenance activity, security history and compatibility.
5. Retrieve the actual source/artifact.
6. Build the candidate in an isolated environment.
7. Benchmark it against M. Engine's existing implementation.
8. Integrate only through an additive boundary (`AUGMENT`, `ADAPTER`, or `COMPOSE`).
9. Run M. Engine's regression/evidence loop.
10. Produce a branch/PR for review rather than silently replacing the native path.

A discovery record is not an integration record, and a benchmark win is not permission to delete the incumbent implementation.

## Current research targets

| Capability area | Candidate | Intended use | Non-destructive mode | Important constraint |
|---|---|---|---|---|
| Concurrency | `kotlinx.coroutines` | Structured concurrency, cancellation and efficient suspension | AUGMENT | Already native to the Kotlin stack; improve usage before adding another abstraction. |
| Resilience | Resilience4j | Retry, circuit breaker, rate limiting, bulkheads and time limits | ADAPTER | Resilience4j 2.x requires Java 17; evaluate for the control plane/remote workers before Android adoption. |
| Durable missions | Temporal Java SDK | Durable workflow execution and recovery | ADAPTER | Treat as a control-plane/remote-worker candidate, not an Android-local dependency by default. |
| Observability | OpenTelemetry Java | Traces, metrics and logs around missions and capability acquisition | AUGMENT | Add telemetry around existing paths; do not replace the Agency Ledger. |
| Static analysis | NullAway | Detect Java nullness defects early | AUGMENT | Targets Java-facing code; complement Kotlin analysis rather than replacing it. |
| UI verification | Showkase | Compose component discovery and visual verification | AUGMENT | Keep existing Roborazzi/screenshot infrastructure and use it as an additional observability surface. |

## Research provenance

* Kotlin structured concurrency: https://kotlinlang.org/docs/coroutines-and-channels.html
* Resilience4j: https://resilience4j.readme.io/ and https://github.com/resilience4j/resilience4j
* Temporal Java SDK: https://github.com/temporalio/sdk-java
* OpenTelemetry Java: https://opentelemetry.io/docs/languages/java/
* NullAway: https://github.com/uber/NullAway
* Showkase: https://github.com/airbnb/Showkase

These sources establish candidate capabilities, not production suitability. M. Engine must independently verify the exact version, license, dependency graph, security posture, build, benchmark and integration evidence before adoption.

## Selection rule

The newest library is not automatically the best library. M. Engine should score:

`evidence × compatibility × maintenance × security × effectiveness × efficiency × reversibility`

and penalize:

`integration cost + dependency surface + runtime risk + lock-in + migration risk`.

A candidate that cannot be physically retrieved, built, exercised and verified remains `BLOCKED_BY_EXTERNAL_DEPENDENCY` or `REAL_BUT_UNVERIFIED` rather than becoming production capability by assertion.

## Mission 11 implementation

This mission adds:

* `OpenSourceEvolutionCatalog` — a typed, provenance-oriented candidate registry.
* `NonDestructiveEvolutionPolicy` — an explicit guardrail that authorizes only additive integration modes by default.
* `NonDestructiveEvolutionPolicyTest` — regression coverage for the policy and catalog integrity.

No existing capability was deleted, disabled, or replaced by this mission.
