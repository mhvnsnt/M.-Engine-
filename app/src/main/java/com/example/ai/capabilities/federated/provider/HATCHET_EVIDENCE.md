# Physical Hatchet Integration Evidence

## Objective
Wire Hatchet into the Universal Provider Fabric as the first real DurableWorkflowProvider, prioritizing physical end-to-end verification over mock assertions.

## Execution Record
1. **HatchetClient Creation:** Created a lightweight Kotlin HTTP client targeting `localhost:8080/api/v1` to establish a physical connection boundary to a running Hatchet instance.
2. **HatchetWorkflowProvider Implementation:** Implemented the `CapabilityProvider` interface. The `probe()` method actively attempts to connect to the Hatchet backend.
3. **Physical Capability Probe (Reality Contract Test):** We dispatched `PhysicalHatchetIntegrationTest` to physically hit the Hatchet endpoints from within the M. Engine environment.

## Observed Result (Reality Contract Enforcement)
The physical probe returned `FabricNodeState.UNAVAILABLE`.
- **Error:** `CAPABILITY_GAP: Hatchet instance unreachable at local endpoint. Physical backend (PostgreSQL/Hatchet Engine) not running.`

As required by the Zero-Config / Progressive Acquisition Rule, we do NOT pretend Hatchet is operational just because the adapter compiles. The physical test explicitly demands actual reachability, which is currently blocked because the requisite Docker/PostgreSQL/RabbitMQ infrastructure is missing in the current AI Studio sandbox container.

## Current State
- `HatchetWorkflowProvider`: **IMPLEMENTED_UNVERIFIED / BLOCKED** by infrastructure `CAPABILITY_GAP`.
- `AutonomousExecutionLoop` (Native): **RETAINED** as the active fallback until Hatchet physically proves durable behavior.
