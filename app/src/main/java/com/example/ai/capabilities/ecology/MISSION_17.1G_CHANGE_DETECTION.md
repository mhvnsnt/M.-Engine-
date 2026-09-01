# Mission 17.1G — Change Detection & Evidence Expiration

## Core Invariant
Evidence belongs to a specific observed state unless explicitly proven transferable. A build result for commit `A` is evidence about commit `A`. It is not automatically evidence that commit `B` builds successfully. 

When a repository changes, existing evidence transitions to `HISTORICAL` and new claims must be established, evaluated, and justified through delta-based reinspection.

## 1. Commit-Bound Evidence
All `HealthDimensionRecord`s explicitly maintain `sourceCommitSha` and an `evidenceStatus` (`CURRENT`, `HISTORICAL`, `STALE`, `REQUIRES_REVALIDATION`, `NOT_TRANSFERABLE`). Old evidence is **never overwritten**—it transitions to `HISTORICAL` and is preserved to maintain the memory ledger.

## 2. Change Detection Engine
Calculates explicit `ChangeDelta`s between two repository snapshots. It categorizes files modified into `ChangeImpact` scopes: `DOCUMENTATION`, `DEPENDENCY`, `SOURCE_STRUCTURE`, `BUILD_SYSTEM`, etc.

## 3. Selective Invalidation (Evidence Transfer)
M. Engine does not arbitrarily wipe the board. 
- If `README.md` changes, `StructuralHealth` transfers with `HIGH_CONFIDENCE`. 
- If `build.gradle` changes, `BuildHealth` requires physical re-execution (transitions to `REQUIRES_REVALIDATION` and `STALE` status).
- If `package.json` changes, `DependencyFreshness` expires immediately.

## 4. Reinspection Planner
Calculates the economic necessity of re-fetching and verifying data:
`Priority = (Relevance * Impact * Consequence * Uncertainty) / Cost`
Outputs: `NO_ACTION`, `LIGHTWEIGHT_RECHECK`, `TARGETED_REINSPECTION`, or `FULL_REINSPECTION`.

## 5. False Positive Guardrails
If network connectivity fails during a scheduled poll, the engine emits an explicitly failed `ChangeDelta`. Old evidence is marked historical, and the new commit receives a confidence of `0.0` with reason `Network inspection failure or missing commit`, preserving epistemic integrity.

## Conclusion
The architecture now possesses a complete, evidence-bounded logic system for handling time and repository mutation. 

**Note on Runtime:** The logic exists completely natively. However, *physical background execution and scheduling* is currently deferred until Mission 17.1H (Persistent Autonomous Runtime), where M. Engine will physically wake up to perform these checks.
