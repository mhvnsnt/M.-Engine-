# Mission 17.2B — Federated Persistent Worker Fabric

## The Reality Constraint
M. Engine cannot be an immortal autonomous system while bound exclusively to an Android application. Mobile operating systems enforce rigid limits (Doze mode, process termination, network restrictions, uninstalls, battery life) that fundamentally conflict with the concept of a 24/7 "living" cognitive architecture.

## The Architectural Shift: Distributed Autonomy
The ultimate goal of this mission is to detach the `AutonomousExecutionLoop` from the mobile device and deploy it as a distributed control plane.

### 1. The Brain (Always-On Control Plane)
- **Infrastructure:** Temporal, Cloudflare Workers, or dedicated Cloud Run services.
- **Function:** Houses the `OpportunityDiscoveryEngine`, the `ExecutionBudget` loop, and the `AutonomyControlPlane`.
- **Persistence:** A PostgreSQL database serving as the Agency Ledger, Evidence Store, and Project Ecology state.

### 2. The Hands (Worker Capability Fabric)
- **Concept:** Replace monolithic local checks with independent, replaceable Worker Nodes.
- **Capabilities:**
  - `RepositoryWorker`: Discovers commits and dependency changes.
  - `ResearchWorker`: Investigates issues, docs, and references.
  - `SandboxWorker`: Executes reversible isolated tests (e.g., via OpenHands).
  - `LocalModelWorker`: Runs inexpensive classifications/summaries via Ollama.
- **Lifecycle:** M. Engine spawns these workers dynamically based on the current objective, collects their `CapabilityResult`, and terminates them. The workers do NOT have independent goals; they are bounded delegates.

### 3. The Interface (Android Command Center)
- **Role:** The mobile application transitions from "the engine" to the **Agency Observatory and Command Surface**.
- **Function:** When the phone wakes or reconnects, it queries the persistent database. It displays the operations M. Engine completed while the user was away, the evidence gathered, the active workers, and any hypotheses awaiting human authorization.
- **Interruption:** It handles Human Development Signals ("Make this feature more fluid"), injecting them directly into the persistent server's Opportunity Queue.

## Execution Strategy
The migration path respects non-destructive evolution:
1. **Phase 1 (Current):** Perfect the bounded loop and Discovery Engine inside the Android process.
2. **Phase 2 (Database):** Migrate the Agency Ledger, Evidence, and State from Android SharedPreferences/Room to a persistent Remote Database.
3. **Phase 3 (Remote Loop):** Port the `AutonomousExecutionLoop` logic to a scheduled cloud infrastructure.
4. **Phase 4 (Observatory):** Strip the Android app of background worker logic and rewire it to sync remotely.
