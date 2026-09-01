# Mission 17.1F — Evidence-Based Project Health

## Core Invariant
M. Engine must diagnose health dimensions independently, not manufacture a single opaque notion of “project health.” A project can be structurally healthy while having failing tests, or inactive but highly stable.

## Multidimensional Matrix
Project health is now a robust evidence matrix where every claim contains:
- `value` (e.g., `UNKNOWN`, `HEALTHY`, `STABLE_LOW_ACTIVITY`)
- `confidence`
- `evidenceReferences`
- `sourceCommitSha`
- `inspectionTimestamp`
- `freshnessPolicy` (default: `EXPIRES_ON_NEW_COMMIT`)
- `falsificationCondition`
- `capabilityGap`

## Health Dimensions Evaluated
1. **Structural Health**: Observability and consistency (e.g., manifests discovered).
2. **Build Health**: Never inferred from structure. Must remain `UNKNOWN` without execution.
3. **Test Health**: Independent of Build Health.
4. **Dependency Freshness**: Distinguishes between outdated, deprecated, and intentionally pinned versions.
5. **Activity**: Activity does not equate directly to health. Resolves into states like `STABLE_LOW_ACTIVITY` or `ABANDONED_CANDIDATE`.
6. **Issue Pressure**: Based on severity and regressions, not simply issue count.
7. **Architectural Complexity**: Descriptive measurement, not an automatic flaw.
8. **Goal Relevance**: Measured explicitly against Owner Objectives.

## Epistemic Guardrails
- **UNKNOWN is Not Failure**: When a dimension is `UNKNOWN` (e.g. `Build Health` without a sandbox), M. Engine does not hallucinate a score or flag an error. It outputs a `CapabilityGap` describing exactly what is missing and how it could be obtained.
- **Strict Evidence Boundaries**: Evidence from commit `A` does not silently transfer to commit `B`. When HEAD changes, old execution evidence is strictly invalidated to prevent historical hallucination.

## Next Action
This matrix positions M. Engine to gracefully calculate `Change Detection & Evidence Expiration` (Mission 17.1G), enabling it to systematically schedule re-evaluations only when economically justified.
