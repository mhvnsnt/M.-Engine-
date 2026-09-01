# Phase A.1: Hatchet Physical Wiring

Goal: Transition the background heartbeat of M. Engine from native coroutines (`AutonomousExecutionLoop`) to a durable Hatchet workflow.

## Steps

1.  **Define Hatchet Client Interface:** Created `HatchetClient.kt` to target `localhost:8080/api/v1`.
2.  **Implement Hatchet Provider Logic:** Implemented `HatchetWorkflowProvider.kt` that probes for the physical instance.
3.  **Physical Verification:** Created `PhysicalHatchetIntegrationTest` to test this boundary without mocks.
4.  **Evidence Recon:** The physical probe correctly identifies the local infrastructure limitation, returning a `FabricNodeState.UNAVAILABLE` with a `CAPABILITY_GAP` rather than mocking a success response.
