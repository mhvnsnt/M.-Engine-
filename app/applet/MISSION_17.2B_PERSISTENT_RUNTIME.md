# Mission 17.2B — Persistent Remote Autonomous Runtime

**M. Engine Phase Evolution**

## The Ultimate Vision
M. Engine must transition from a bounded loop executing inside the Android mobile application into a **Tandem Development Runtime** that operates on resilient remote cloud infrastructure. 

**The Android app is not the engine; it is the Agency Observatory and Command Surface.**

The actual autonomous metabolism — the continuous loop of discovering opportunities, scheduling specialist workers, updating evidence, and recording decisions — lives on an always-on remote server (The Brain), dispatching distributed workers (The Hands).

## The Four Inputs of the Tandem Runtime

1. **Human Signal Stream:** The owner’s creative conversation streams (e.g., chat signals like `NEW_REQUIREMENT` or `IDEA`) which are structured as `DevelopmentSignal` objects and pushed to the Remote Runtime’s queue.
2. **Autonomous Opportunity Loop:** A scheduled background thread (the Governor) that checks for environmental changes, stale data, blockages, or unverified evidence and plans authorized `AutonomousCycleBudget`-constrained iterations.
3. **Shared Development Memory:** A PostgreSQL-backed persistence layer linking `Agency Ledger`, `Evidence Graph`, and `Project State`, ensuring both the human and autonomous workers write to the same truth structure.
4. **The Agency Observatory (Android):** The edge client that merely reads the `Control Plane State`, `Mindstream`, and active `Federated Workers`, surfacing M. Engine's activity visually and allowing pause/resume operations.

## Layered Architecture Implementation

### Phase 1 — Move State Off the Phone (Completed)
- **Objective:** Provision `PostgreSQL` as the single source of truth for the Agency Ledger.
- **Tables Created:**
  - `control_plane_state`: Master kill-switch and autonomy toggle.
  - `agency_runs`: Bounded autonomous cycles.
  - `mindstream_entries`: Operational decision stream.
  - `opportunities`: Unresolved tasks and objectives.
  - `development_signals`: Mapped requests generated from human input.
  - `evidence_ledger`: Verified facts.
  - `project_ecology`: Cached system architecture.
  - `worker_results`: Results of independent capability node delegations.

### Phase 2 — The Remote Scheduler (Active)
- **Objective:** Construct the Cloud-based Autonomous Metabolism to replace WorkManager-driven local ticks.
- **Component:** `AutonomousMetabolismScheduler.kt` and `CycleExecutor.kt`
- **Logic:** Execute a budgeted autonomous cycle based on periodic heartbeat signals. Ensure the loop does not run forever, but within a defined `AutonomousCycleBudget` (e.g., max 10 actions, 5 min runtime).

### Phase 3 — Durable Workflows
- **Objective:** Integrate workflow engines (like Temporal or Cloudflare Workers) so long-running operations survive process terminations.
- **Details:** Relegate multi-step orchestration (e.g., investigate -> spawn sandbox -> wait for github -> compile -> verify) to robust workflow graphs instead of chained local coroutines.

### Phase 4 — Federated Capability Workers
- **Objective:** Replace monolithic code execution with transient specialist actors.
- **Workers:** 
  - `RepositoryWorker` (Code/Commit analysis)
  - `ResearchWorker` (Documentation reading)
  - `SandboxWorker` (Executing tests)
  - `LocalModelWorker` (Low-cost classification via Ollama)

### Phase 5 — Wire the Observatory
- **Objective:** Connect the Android application strictly as a reader and manual governor of this remote brain.
- **Details:** Uses the `RemoteEndpointConfiguration` and `RemoteControlPlaneRepository` previously developed in 17.2C.

## Operating Principles
- **No Infinite Agent Swarms:** M. Engine remains the sole decision authority. Workers are temporary, bounded executors spawned for a specific goal. They do not have their own continuous goals.
- **Tandem Reality:** M. Engine works while the owner works elsewhere. Synchronization happens through the Shared Memory Layer.
