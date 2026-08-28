import re

# Update REALITY_CONTRACT.md
with open("REALITY_CONTRACT.md", "r") as f:
    contract = f.read()

phase20_rules = """
## MODELS PROPOSE. REALITY DECIDES. (Phase 20)
> A model can say "I fixed the bug." Evidence can say "The bug still reproduces." Evidence wins.
> "The build passed" does not mean "The application works."
> Every claim must be backed by evidence tied to a specific commit, environment, test, observation, and result.

## FIRST-CLASS CONNECTORS
> GitHub and other services must act as external capability providers authenticated via official delegated flows (e.g., OAuth, GitHub App installation). Paste-a-PAT is rejected as a long-term architecture.
"""
contract += phase20_rules

with open("REALITY_CONTRACT.md", "w") as f:
    f.write(contract)

# Update CAPABILITY_LEDGER.md
with open("CAPABILITY_LEDGER.md", "r") as f:
    ledger = f.read()

ledger = re.sub(
    r"Replaced stubbed REST API modifications with real local clone/commit/push capabilities using JGit. Tested via compilation. Eliminates the need for fake remote sandboxes.",
    "Provides real local clone/commit/push capabilities using JGit. NOTE: This provides real Git operations, but does not by itself provide a complete local developer environment. Verified via compilation.",
    ledger
)

classifications = """
## 6. Phase 20 Reality Classifications
| Subsystem | Current Classification | Note |
| :--- | :--- | :--- |
| **Git Operations (JGit)** | `REAL_AND_CONNECTED` | Requires verification against live remote repo. |
| **Web Client / PWA** | `REAL_BUT_UNVERIFIED` | API exists; awaits real web deployment. |
| **Remote Worker Boundary**| `REAL_BUT_UNCONFIGURED` | Orchestrator exists; awaits physical SWE/OpenHands worker attachment. |
| **Physical Device (ADB)** | `BLOCKED` | Awaits actual reachable device gateway. |
| **Self-Development** | `PARTIAL` | Loop is built, but awaits first end-to-end self-modifying mission completion. |
"""
ledger += classifications

with open("CAPABILITY_LEDGER.md", "w") as f:
    f.write(ledger)

