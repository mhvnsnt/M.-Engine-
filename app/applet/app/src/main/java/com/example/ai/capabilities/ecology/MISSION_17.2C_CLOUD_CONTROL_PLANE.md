# MISSION 17.2C — PERSISTENT AUTONOMOUS CLOUD CONTROL PLANE

## Architectural Principle
M. Engine must survive independently of the Android application's lifecycle. Android and the PWA are observer/control surfaces, not the sole execution environment.

Do not replace or destructively refactor the existing Android autonomous architecture. Extend it non-destructively through a federated cloud control plane.

## 1. Canonical Persistent Agency Ledger
Build a canonical persistent Agency Ledger backed by PostgreSQL. The ledger must persist:
- Agency runs and cycles
- Agency boundary state transitions
- Owner objectives and goal vectors
- Development signals
- Opportunities and rankings
- Knowledge claims
- Evidence and provenance
- Belief revisions
- Knowledge conflicts
- Repository snapshots
- Project health evidence
- Dependency graph edges
- Research artifacts
- Experiments and results
- Capability states and gaps
- Worker jobs and worker results
- Budget consumption
- Mindstream entries
- Approval requests
- Kill switch state

The system must support recovery after: Android process death, phone shutdown, worker crash, server restart, temporary network loss.

## 2. Cloud Autonomous Metabolism Scheduler
Implement a cloud Autonomous Metabolism Scheduler. The scheduler lifecycle must be:
`RECOVER_STATE → CHECK_KILL_SWITCH → CHECK_BUDGET → CHECK_OWNER_OBJECTIVES → CHECK_SCHEDULED_MONITORING → DETECT_EVIDENCE_CHANGES → GENERATE_OPPORTUNITIES → RANK_OPPORTUNITIES → SELECT_AUTHORIZED_WORK → SPAWN_REQUIRED_WORKERS → COLLECT_EVIDENCE → RECONCILE_CONTRADICTIONS → RUN_AUTHORIZED_EXPERIMENTS → RECORD_RESULTS → UPDATE_EPISTEMIC_MEMORY → EMIT_MINDSTREAM → SCHEDULE_NEXT_WAKE`

## 3. Worker Constraints
No autonomous action may claim completion without corresponding persisted `EvidenceOfAction`.
`UNKNOWN` must remain a valid result.
Workers are federated capabilities, not independent authorities.
The Governor remains the sole component authorized to transition high-level agency state.

All workers must:
- receive explicit objective scope
- receive explicit authorization scope
- receive a bounded budget
- return structured evidence
- report capability limitations
- be independently cancellable
- be revocable
- be prevented from silently escalating their scope

## 4. Observatory & Control Surface
Implement a distributed kill switch that can be triggered from Android, PWA, or the control plane.
The mobile application should synchronize with the canonical ledger and display the Mindstream in near real time.
Do not expose private chain-of-thought. Instead expose:
`OBSERVED`, `INFERENCE`, `INTENT`, `EXPERIMENT`, `RESULT`, `DECISION`, `NEXT ACTION`, `CAPABILITY GAP`, `AUTHORIZATION LEVEL`, `EVIDENCE REFERENCES`, `BUDGET CONSUMPTION`

## Verification
After implementation, perform physical tests proving:
1. An autonomous cycle survives client disconnect.
2. A queued worker job survives scheduler restart.
3. Duplicate jobs are prevented through idempotency keys.
4. Kill switch cancels active authorized work.
5. Evidence remains attached to immutable repository snapshots.
6. UNKNOWN is preserved rather than converted into success.
7. Android can reconnect and recover the live Agency Observatory.
