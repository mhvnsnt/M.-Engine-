# Physical OpenHands Integration Evidence

## Objective
Wire OpenHands into the Universal Provider Fabric as the primary `CodingWorkerProvider`, ensuring physical connectivity enforcement.

## Execution Record
1. **OpenHandsClient Creation:** Created a Kotlin HTTP client targeting `localhost:3000/api/v1` to establish a physical connection to a running OpenHands Agent Canvas server.
2. **OpenHandsCodingProvider Implementation:** Implemented `CapabilityProvider`. The probe actively checks for the server, and the execution phase maps M. Engine's internal task model to an OpenHands session payload (JSON).
3. **Physical Capability Probe (Reality Contract Test):** Executed `PhysicalOpenHandsIntegrationTest` to verify that the M. Engine environment accurately senses physical OpenHands availability.

## Observed Result
The physical probe returned `FabricNodeState.UNAVAILABLE`.
- **Error:** `CAPABILITY_GAP: OpenHands instance unreachable at local endpoint. Physical backend not running.`

Because the OpenHands daemon is not physically running in this sandbox, M. Engine correctly blocks the capability rather than simulating success.

## Current State
- `OpenHandsCodingProvider`: **IMPLEMENTED_UNVERIFIED / BLOCKED** by infrastructure `CAPABILITY_GAP`.
- `CodeMutationEngine` (Native Kotlin AST Manipulator): **RETAINED** as the fallback coding engine until OpenHands proves full physical viability in the target environment.
