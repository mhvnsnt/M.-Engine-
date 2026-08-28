import os
import re

# 1. Update CapabilityGraph.kt
with open("app/src/main/java/com/example/ai/capabilities/CapabilityGraph.kt", "r") as f:
    content = f.read()

content = content.replace(
    "REMOTE_SERVICE,",
    "REMOTE_SERVICE,\n    REMOTE_WORKER,\n    EXTERNAL_GATEWAY,"
)

with open("app/src/main/java/com/example/ai/capabilities/CapabilityGraph.kt", "w") as f:
    f.write(content)

# 2. Update REALITY_CONTRACT.md
with open("REALITY_CONTRACT.md", "r") as f:
    contract = f.read()

phase19_rules = """
## EXTERNAL EXECUTION & WEB BOOTSTRAP (Phase 19)
> M. Engine must not be trapped in an Android APK. The Android application and Web/PWA interface are merely clients to the durable M. Engine Shared Control Plane.
> Python/Node-based autonomous agents (e.g., SWE-agent, OpenHands, Aider) are NOT rejected because they are non-Kotlin. They are explicitly integrated as **Remote Workers** dispatched by the Worker Orchestrator.
> Simulated device interaction is strictly prohibited. Physical device observation/action must be routed through an explicit **Device Gateway** boundary (ADB/UIAutomator).
"""
contract += phase19_rules

with open("REALITY_CONTRACT.md", "w") as f:
    f.write(contract)

# 3. Update CAPABILITY_LEDGER.md
with open("CAPABILITY_LEDGER.md", "r") as f:
    ledger = f.read()

# Remove SWE-agent from rejected
ledger = re.sub(
    r"\| \*\*SWE-agent \/ OpenHands\*\* \| Python/Node-based; cannot be embedded directly into the M\. Engine Kotlin/Android core without a remote execution API\. \| August 27, 2026 \|\n",
    "",
    ledger
)

# Add them to Verified/Remote Workers (New Section)
remote_workers = """
## 5. Remote Workers & External Execution Boundaries
| Capability | Current Strategy | Integration Boundary | Reality Classification |
| :--- | :--- | :--- | :--- |
| **Python/Node Autonomous Agents** (SWE-agent, OpenHands, Aider) | Expose `RemoteWorkerOrchestrator` to delegate jobs to external isolated execution environments. | `REMOTE_WORKER` | `ESTABLISHED_BOUNDARY` - Ready for external worker connection. |
| **Physical Device Actuators** | Dispatch UI testing/device control commands through `DeviceGateway`. | `EXTERNAL_GATEWAY` | `ESTABLISHED_BOUNDARY` - Replaced fake local simulator. |
| **Web/PWA Client** | `m-engine-web/` directory initialized. Consumes Shared Control Plane API. | Standalone Deployment | `ESTABLISHED_BOUNDARY` |
"""
ledger += remote_workers

with open("CAPABILITY_LEDGER.md", "w") as f:
    f.write(ledger)

