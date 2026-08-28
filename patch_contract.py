with open("REALITY_CONTRACT.md", "r") as f:
    content = f.read()

import re

# Update auth rule
content = re.sub(
    r"8\. \*\*No Long-Lived Machine Credentials:\*\*.*",
    "8. **No Long-Lived Machine Credentials:** Manual secrets are an exceptional fallback, not the normal connection mechanism. Use delegated authentication and workload identity wherever the provider supports it. For Firebase/GCP CI, GitHub Actions' OIDC identity MUST be exchanged for short-lived Google credentials rather than storing a long-lived service-account key.",
    content
)

# Add Mission state rule
mission_rule = """
## DURABLE MISSION STATE (Phase 17)
> Every user request is classified as either a conversation, a mission, or an explicit instruction to perform an immediate action.
Development, research, debugging, repository modification, deployment, self-improvement, and long-running objectives must become durable Missions rather than ephemeral LLM conversations.
A client disconnect, browser closure, Android process termination, worker failure, network interruption, or model replacement must not destroy the Mission's authoritative state.
"""
content += mission_rule

with open("REALITY_CONTRACT.md", "w") as f:
    f.write(content)
