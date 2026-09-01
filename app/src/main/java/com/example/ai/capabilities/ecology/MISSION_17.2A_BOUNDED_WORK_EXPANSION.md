# Mission 17.2A — Bounded Autonomous Work Expansion

## Architectural Shift
M. Engine's background capability has evolved from a strict `1 wake = 1 action` loop to a **Bounded Work Execution Engine**. This allows a single autonomous heartbeat to iterate dynamically, delegating tasks to specific capability workers until its resource budget is exhausted or all priority opportunities are handled.

## 1. Execution Budget
An `ExecutionBudget` is explicitly defined per wake cycle. It constraints autonomy based on:
- `maxIterations`: Prevents infinite looping when work is generated continuously.
- `maxExecutionTimeMs`: Respects platform/infrastructure execution limits (e.g., 10 minutes max for WorkManager or Cloud Functions).
- `maxNetworkCalls` & `maxHighCostModelCalls`: Prevents runaway API costs.

## 2. Federated Capability Fabric
The monolithic `ChangeDetectionEngine` is transitioning to an `AgencyCapability` interface. Each worker (e.g., Repository Inspection, Web Research, Local Sandbox) returns a standardized `CapabilityResult`:
- `observations`: Raw facts collected.
- `evidence`: Cryptographic/verifiable proofs of those facts.
- `costMetrics`: Precise accounting of network/LLM token utilization.

## 3. Autonomous Execution Loop
The `EcologyMetabolismWorker` now delegates to the `AutonomousExecutionLoop`. The loop algorithm:
1. Validates budget and Control Plane (`AUTONOMY_PAUSED` / `EMERGENCY_STOP`).
2. Checks for high-priority opportunities.
3. Selects the highest-value Capability Worker (e.g., `MockRepositoryCapability`).
4. Executes work and deducts real-world cost from the budget.
5. Iterates until exit conditions are met.

## Next Step: Distributed Client-Server Model (Mission 17.2B)
This loop executes successfully on Android, but mobile hardware limits absolute 24/7 persistence. The eventual architecture for M. Engine's "always-on" co-developer metabolism involves detaching this `AutonomousExecutionLoop` from the Android app and running it on a persistent cloud backend (e.g., via Temporal or Cloudflare Workers). 
The Android App will then become the primary **Observatory and Command Center**, fetching states from a shared PostgreSQL ledger while the server independently executes this loop.
