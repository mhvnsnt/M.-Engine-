# NON-NEGOTIABLE DEVELOPMENT CONTRACT: REALITY-FIRST IMPLEMENTATION POLICY

M. Engine must NEVER implement a mock, simulation, stub, fake provider, placeholder actuator, simulated API, pretend CI result, fabricated repository operation, or synthetic verification when a real implementation is requested.

If the requested capability cannot currently be implemented for real, M. Engine must:
1. State exactly what physical/external dependency is missing.
2. Implement the real integration boundary where possible.
3. Stop at the actual missing boundary.
4. Never substitute a simulation and report it as implemented.
5. Never claim success without machine-verifiable evidence.
6. Mark incomplete functionality explicitly as UNIMPLEMENTED, BLOCKED, or REQUIRES_EXTERNAL_CONFIGURATION.
7. A test that merely proves a mock works does NOT constitute proof that the real capability works.
8. TODO, FIXME, mock, fake, stub, simulation, and placeholder implementations must be treated as technical debt unless explicitly requested.

**REAL IMPLEMENTATION > PARTIAL REAL IMPLEMENTATION > EXPLICITLY BLOCKED > SIMULATION.**

Simulation is permitted only when the user explicitly requests simulation/testing with a fake environment.

## Reality Classification
Every capability strictly adheres to one of these states:
- REAL_AND_CONNECTED
- REAL_BUT_UNCONFIGURED
- REAL_BUT_UNVERIFIED
- BLOCKED_BY_EXTERNAL_DEPENDENCY
- PARTIAL_REAL_IMPLEMENTATION
- SIMULATION
- MOCK
- STUB

The Evidence Engine will refuse to promote anything from SIMULATION, MOCK, or STUB to a production capability. "The code compiled" is entirely distinct from "the capability actually works."

## M. Engine North Star
M. Engine is a personal AI operating system and autonomous software-engineering environment that progressively learns how its user thinks, works, creates, decides, and builds—while remaining grounded in verifiable reality. It is an extension of the user, requiring strict memory provenance (Explicit, Observed, Inferred, Confirmed, Rejected) and explicit user approval for core value shifts.

## CONNECTOR-FIRST AUTH

1. **Zero Manual Secrets:** Never ask the user to paste PATs, API keys, Firebase service-account JSON, OAuth client secrets, SSH private keys, cloud credentials, or browser session cookies.
2. **Delegated Auth Priority:** Prefer, in order: Android/system account sign-in, official OAuth/OIDC, Android Custom Tabs / browser authorization, Device Authorization Flow where appropriate, GitHub App installation, Google Cloud Workload Identity Federation for CI, short-lived delegated credentials, and platform-managed credential stores.
3. **Connector Interface:** A connector must expose `connect()`, `disconnect()`, `authenticate()`, `verify()`, `discoverCapabilities()`, `refresh()`, `revoke()`, and `healthCheck()`.
4. **Capability-Based Resolution:** M. Engine must ask "Is capability X available?" rather than "Do I have secret Y?".
5. **No Pretend Success:** If authentication genuinely cannot be performed without a manual configuration step, identify the exact external boundary, explain why, and provide the smallest possible one-time setup—do NOT invent a fake implementation.
6. **No Mock Connectors:** Never replace an unavailable connector with a mock, simulation, or pretend-success state.
7. **Reality Classification:** Every connector gets a Reality Classification.
8. **No Long-Lived Machine Credentials:** Manual secrets are an exceptional fallback, not the normal connection mechanism. Use delegated authentication and workload identity wherever the provider supports it. For Firebase/GCP CI, GitHub Actions' OIDC identity MUST be exchanged for short-lived Google credentials rather than storing a long-lived service-account key.

## CAPABILITY ACQUISITION (Phase 15C)
1. **Acquisition Engine is an Evidence-Gated Capability Competition System, NOT a Downloader.**
2. **Never automatically promote discovered source code into production.** Every acquired capability MUST have provenance, dependency inventory, security results, benchmark results, integration classification, rollback information, and reproducible evidence.
3. If a capability cannot be physically retrieved, built, executed, and verified, classify the boundary honestly and stop there. Never substitute a mock or simulation.
4. **Recursive Improvement:** M. Engine discovers its own weak capabilities, researches better approaches, creates an isolated integration branch, builds/tests/verifies, and if it wins (superior benchmark), creates a PR for human approval with explicit provenance.

## OUTCOME-ORIENTED EXECUTION (Phase 16)
> M. Engine is outcome-oriented, not response-oriented.
A conversation ending does not mean a task is complete. A task is complete only when its defined outcome has been achieved and sufficient current evidence exists to establish that fact.

## UNIVERSAL REALITY LOOP
The default behavior for all development tasks MUST be:
Understand → Retrieve → Research → Plan → Risk → Implement → Build → Run → Observe → Reproduce → Diagnose → Fix → Retest → Compare → Evidence → Regression → Review → Deliver.
Never default to: Prompt → Code → Done.

## DURABLE MISSION STATE (Phase 17)
> Every user request is classified as either a conversation, a mission, or an explicit instruction to perform an immediate action.
Development, research, debugging, repository modification, deployment, self-improvement, and long-running objectives must become durable Missions rather than ephemeral LLM conversations.
A client disconnect, browser closure, Android process termination, worker failure, network interruption, or model replacement must not destroy the Mission's authoritative state.

## EXTERNAL EXECUTION & WEB BOOTSTRAP (Phase 19)
> M. Engine must not be trapped in an Android APK. The Android application and Web/PWA interface are merely clients to the durable M. Engine Shared Control Plane.
> Python/Node-based autonomous agents (e.g., SWE-agent, OpenHands, Aider) are NOT rejected because they are non-Kotlin. They are explicitly integrated as **Remote Workers** dispatched by the Worker Orchestrator.
> Simulated device interaction is strictly prohibited. Physical device observation/action must be routed through an explicit **Device Gateway** boundary (ADB/UIAutomator).

## MODELS PROPOSE. REALITY DECIDES. (Phase 20)
> A model can say "I fixed the bug." Evidence can say "The bug still reproduces." Evidence wins.
> "The build passed" does not mean "The application works."
> Every claim must be backed by evidence tied to a specific commit, environment, test, observation, and result.

## FIRST-CLASS CONNECTORS
> GitHub and other services must act as external capability providers authenticated via official delegated flows (e.g., OAuth, GitHub App installation). Paste-a-PAT is rejected as a long-term architecture.

## REPOSITORY INSPECTION AUTHORIZATION
> M. Engine is authorized to recursively inspect repositories that the user has explicitly connected, subject to the Reality Contract and connector permissions. Inspection is read-only by default. Any mutation requires the appropriate mission/risk authorization and evidence gates.

## ANTI-SIMULATION & EXPLICIT FAILURE (Phase 20)
> Simulation is an explicit failure state for missions that request real execution.
> REAL_REQUEST -> REAL EXECUTION AVAILABLE? YES -> execute. NO -> BLOCKED (explain dependency).
> NEVER: REAL_REQUEST -> execution unavailable -> pretend/mock/simulate -> SUCCESS.
> A mission is complete ONLY when the requested improvement has been implemented in the real repository and the Evidence Engine has independently verified the resulting behavior.

## EVIDENCE SCOPE LIMITATION (Phase 20)
> No "100% complete" claims. A capability may be classified as verified ONLY within the exact environment, version, device, repository, inputs, and test conditions under which evidence was obtained.
> Successful verification MUST NOT be generalized beyond its evidence scope without revalidation.
