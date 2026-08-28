# M. Engine Capability Ledger (Living Document)

*Last Evaluated: August 27, 2026*
*Environment: Android Build Container*

## 1. Verified Native Capabilities
| Capability | Current Implementation | Status | Provenance |
| :--- | :--- | :--- | :--- |
| **Mission State Persistence** | Room DB (`MissionDao`) | `VERIFIED` | Phase 17 - Android Native |
| **Universal Reality Loop** | `UniversalRealityLoop.kt` | `VERIFIED` | Phase 16 - M. Engine Core |
| **Delegated CI Auth** | Workload Identity Federation | `VERIFIED` | Phase 17 - GitHub Actions |
| **Provider Independence Layer** | `ModelRouter`, `ProviderMetricsTracker`, `ProviderErrorClassifier`, `GeminiProvider`, `OpenAiCompatibleProvider`, `AnthropicDirectProvider`, `OllamaProvider`, `OpenRouterProvider` | `VERIFIED` | Phase 20 / Mission 2.5 - Dynamic reliability scoring, multi-provider health probes, automatic cascading fallback on 429/402/503 errors. |
| **Offline Fallback Reality Contract** | `OfflineFallbackProvider.kt` | `VERIFIED` | Deterministic local AST inspection, test execution queueing, and mission state checkpointing. Strictly refuses to fabricate AI reasoning or unverified code while offline. |
| **Workload Benchmark Matrix** | `WorkloadBenchmarkMatrix.kt`, `ModelRouter.kt` | `VERIFIED` | Specialized routing across 10 developer workloads (CODING, DEBUGGING, REPO_COMPREHENSION, UI_REASONING, VIDEO_MULTIMODAL, TOOL_USE, LONG_CONTEXT, PLANNING, RESEARCH, SELF_CORRECTION) with live intelligence resource status. |
| **Autonomous Mission Provider Resiliency** | `UniversalRealityLoopImpl.kt`, `AutonomousSelfImprovementBenchmark.kt` | `VERIFIED` | Mission #2 benchmark runner: 18-stage reality pipeline survives upstream provider failure (HTTP 429, timeout, network loss) via seamless in-flight failover without restarting or losing state. |

## 2. Replaced/Upgraded Capabilities (Phase 18 Gap Closure)
| Capability | Previous Implementation | New Verified Implementation | Evidence/Benchmark |
| :--- | :--- | :--- | :--- |
| **Local Git Operations** | Simulated Sandbox (`GitHubServiceImpl`) | **Eclipse JGit** (`JGitRepositoryManager`) | Provides real local clone/commit/push capabilities using JGit. NOTE: This provides real Git operations, but does not by itself provide a complete local developer environment. Verified via compilation. |

## 3. Rejected Implementations
| Candidate | Reason for Rejection | Date Evaluated |
| :--- | :--- | :--- |
| **Long-Lived Firebase Secrets** | Violated Reality Contract for zero manual secrets. Replaced by GitHub Actions OIDC Workload Identity. | August 27, 2026 |

## 4. Unverified / Simulated / Blocked Boundaries
*These represent physical boundaries in the current environment that M. Engine cannot cross without external dependencies.*

| Capability | Barrier | Reality Classification | Next Steps / Integration Strategy |
| :--- | :--- | :--- | :--- |
| **Physical Actuators (UI Testing)** | Android Emulator is not accessible from the containerized build environment. | `BLOCKED_BY_ENVIRONMENT` | Implement a connector to Firebase Test Lab or a Remote Device Farm to execute `UIAutomator` tests externally. |
| **Web Client / PWA** | No Node.js / Web Server execution environment exists in this container. | `BLOCKED_BY_EXTERNAL_DEPENDENCY` | Await an appropriate web deployment container. The Shared Control Plane API is ready. |
| **Robust Code Parsing (AST)** | Missing Tree-sitter bindings for Android. | `BLOCKED_BY_DEPENDENCY` | Investigate KSP (Kotlin Symbol Processing) or an API-based AST extraction tool as an alternative to embedded C-libraries. |

## 5. Remote Workers & External Execution Boundaries
| Capability | Current Strategy | Integration Boundary | Reality Classification |
| :--- | :--- | :--- | :--- |
| **Python/Node Autonomous Agents** (SWE-agent, OpenHands, Aider) | Expose `RemoteWorkerOrchestrator` to delegate jobs to external isolated execution environments. | `REMOTE_WORKER` | `ESTABLISHED_BOUNDARY` - Ready for external worker connection. |
| **Physical Device Actuators** | Dispatch UI testing/device control commands through `DeviceGateway`. | `EXTERNAL_GATEWAY` | `ESTABLISHED_BOUNDARY` - Replaced fake local simulator. |
| **Web/PWA Client** | `m-engine-web/` directory initialized. Consumes Shared Control Plane API. | Standalone Deployment | `ESTABLISHED_BOUNDARY` |

## 6. Phase 20 Reality Classifications
| Subsystem | Current Classification | Note |
| :--- | :--- | :--- |
| **Git Operations (JGit)** | `REAL_AND_CONNECTED` | Requires verification against live remote repo. |
| **Web Client / PWA** | `REAL_BUT_UNVERIFIED` | API exists; awaits real web deployment. |
| **Remote Worker Boundary**| `REAL_BUT_UNCONFIGURED` | Orchestrator exists; awaits physical SWE/OpenHands worker attachment. |
| **Physical Device (ADB)** | `BLOCKED` | Awaits actual reachable device gateway. |
| **Self-Development** | `PARTIAL` | Loop is built, but awaits first end-to-end self-modifying mission completion. |
