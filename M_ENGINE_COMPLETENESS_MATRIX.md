# M. ENGINE COMPLETENESS MATRIX

Generated 2026-09-01. **Every number here is measured, not asserted.**

Reproduce with:

```bash
node scripts/audit/reachability.cjs   # measures reachability + reality markers
node scripts/audit/gen_matrix.cjs     # regenerates the subsystem table below
```

---

## Method, and its limits

`REALITY_CONTRACT.md` forbids claiming a capability exists because an interface
exists. So the question asked of every declaration is not *"was it written?"* but
*"is it reachable from something that actually runs?"*

`scripts/audit/reachability.cjs` indexes every top-level declaration — types
**and** functions — across the production source sets, builds a reference graph,
and breadth-first searches from the two real process entry points:

- `app/src/main/java/com/example/MainActivity.kt` (Android)
- `cloud_control_plane/src/main/kotlin/com/example/ai/cloud/Main.kt` (control plane)

**This over-approximates reachability.** A mere mention in a comment creates an
edge. That makes DISCONNECTED findings *conservative*: if this reports something
unreachable, nothing in the tree names it at all.

**What it cannot tell you.** Reachability is necessary, not sufficient. Code can
be reachable and still be wrong, dead behind a runtime flag, or simulated. The
marker scan is a second axis, not a proof of correctness. Nothing in this
document should be read as "verified working" — see
`M_ENGINE_REALITY_VERIFICATION_REPORT.md` for the far shorter list of things
actually demonstrated end to end.

An instrument bug found and fixed during this audit is worth recording: the first
version indexed only `class`/`object`/`interface`, and every Jetpack Compose
screen in this codebase is a top-level `fun`. It confidently reported all of them
disconnected while `AppShell.kt:148` calls them directly. A reachability result is
only as good as its declaration index.

---

## Headline

| Measure | Value |
| --- | ---: |
| Production Kotlin files | 288 |
| Production LOC | 31,513 |
| Top-level declarations | 831 |
| **Reachable from an entry point** | **208 files (73.3%)** |
| **DISCONNECTED** | **77 files, ~5,414 LOC (26.7%)** |
| Files carrying simulation markers | 49 |
| Total simulation markers | 97 |

Roughly **one line in six of this codebase has never been able to execute** (was one in five before the canonicalization pass).

That is the actual bottleneck, and it is not a shortage of code. It is the gap
between written and wired.

---

## Subsystem table

| Subsystem (package) | Files | LOC | Reachable | Real I/O | Sim markers | Measured state |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| `ai/capabilities` | 69 | 9219 | 49/69 | 13 | 44 | PARTIAL / MARKERS PRESENT |
| `ui` | 20 | 6059 | 19/20 | 2 | 12 | PARTIAL / MARKERS PRESENT |
| `ai/capabilities/ecology` | 40 | 5283 | 19/40 | 6 | 12 | PARTIAL / MARKERS PRESENT |
| `data` | 39 | 1812 | 38/39 | 2 | 0 | PARTIALLY CONNECTED |
| `ai` | 16 | 1735 | 15/16 | 2 | 2 | PARTIAL / MARKERS PRESENT |
| `ai/cloud` | 7 | 1192 | 7/7 | 3 | 2 | IMPLEMENTED_UNVERIFIED (markers present) |
| `ai/capabilities/federated/provider` | 17 | 906 | 16/17 | 14 | 3 | PARTIAL / MARKERS PRESENT |
| `ai/capabilities/federated/environment` | 9 | 687 | 6/9 | 1 | 3 | PARTIAL / MARKERS PRESENT |
| `ai/capabilities/memory` | 9 | 616 | 5/9 | 1 | 1 | PARTIAL / MARKERS PRESENT |
| `ai/capabilities/federated` | 7 | 523 | 2/7 | 3 | 4 | PARTIAL / MARKERS PRESENT |
| `network` | 9 | 518 | 8/9 | 1 | 0 | PARTIALLY CONNECTED |
| `ai/capabilities/acquisition` | 5 | 504 | 4/5 | 1 | 1 | PARTIAL / MARKERS PRESENT |
| `ai/capabilities/directed` | 10 | 493 | 0/10 | 1 | 3 | DISCONNECTED |
| `ai/capabilities/connections` | 6 | 353 | 6/6 | 0 | 4 | IMPLEMENTED_UNVERIFIED (markers present) |
| `github` | 2 | 289 | 1/2 | 0 | 0 | PARTIALLY CONNECTED |
| `ai/capabilities/epistemic` | 2 | 220 | 2/2 | 0 | 0 | CONNECTED (in-process only) |
| `ai/capabilities/multimodal` | 3 | 205 | 0/3 | 0 | 3 | DISCONNECTED |
| `ai/capabilities/tandem` | 4 | 184 | 4/4 | 0 | 1 | IMPLEMENTED_UNVERIFIED (markers present) |
| `ai/capabilities/evolution` | 2 | 149 | 2/2 | 0 | 0 | CONNECTED (in-process only) |
| `other` | 2 | 146 | 1/2 | 0 | 0 | PARTIALLY CONNECTED |
| `ui/theme` | 3 | 142 | 1/3 | 0 | 0 | PARTIALLY CONNECTED |
| `ai/capabilities/integration` | 2 | 124 | 0/2 | 0 | 2 | DISCONNECTED |
| `ai/capabilities/boundary` | 3 | 121 | 1/3 | 0 | 0 | PARTIALLY CONNECTED |
| `ai/capabilities/workspace` | 1 | 109 | 1/1 | 0 | 0 | CONNECTED (in-process only) |
| `ai/capabilities/mutation` | 1 | 78 | 1/1 | 0 | 0 | CONNECTED (in-process only) |
| **TOTAL** | **288** | **31667** | **208/288** | | **97** | |

---

## Memory: the crown jewels, now partly connected

The previous revision of this document reported `ai/capabilities/memory` as
**100% disconnected** — the Level 0 ledger, context reconstruction, owner context
and ontology federation all unreachable while the product ran conversations
through a separate Room path.

**Measured now: 5 of 9 files reachable, 4 with real data flowing.**

| File | Reachable | Real data flow | Physical verification | State |
| --- | --- | --- | --- | --- |
| `ImmutableConversationLedger` | YES | YES | Survives DB close/reopen | **PARTIALLY_VERIFIED** |
| `ContextReconstructionEngine` | YES | YES | Consumes hydrated prefs + excludes superseded events | **PARTIALLY_VERIFIED** |
| `OwnerContextGraph` | YES | YES | Hydrated from `terminology_preferences` | **PARTIALLY_VERIFIED** |
| `OntologyFederation` | YES | constructed | Categories present, no claims flowing | IMPLEMENTED_UNVERIFIED |
| `FileBackedConversationLedger` | *false positive* | NO | — | **OBSOLETE** — named only in comments |
| `SemanticResearchGraph` | NO | NO | — | DISCONNECTED (Level 4) |
| `ResearchHistoryEngine` | NO | NO | — | DISCONNECTED |
| `ResearchMemoryState` | NO | NO | — | DISCONNECTED |
| `MemoryIndependenceCheck` | NO | NO | — | DISCONNECTED (Level 6) |

Gap: **Levels 2, 4 and 6 do not exist.** There is no project memory, no semantic
retrieval on the canonical path, and no meta-memory. "Memory works" would be an
overstatement; Level 0, Level 1 and Level 5 work.

Still fully disconnected: `ai/capabilities/directed` (10 files),
`ai/capabilities/multimodal` (3), `ai/capabilities/integration` (2).

---

## Directive sections against measured reality

States use the vocabulary of `REALITY_CONTRACT.md`.

| § | Capability | Measured state | Evidence |
| --- | --- | --- | --- |
| 3 | Universal Sidebar / Home | **PARTIAL** | The drawer is real and routes work. 5 of 10 destinations are literally `Text("… (WIP)")`: Home, Apps, Games, Workspaces, Agents |
| 4 | Persistent conversations | **PARTIAL_REAL_IMPLEMENTATION** | Level 0 ledger now canonical and restart-verified. Still no titles, search, folders, pinning, archival, branching |
| 5 | Project workspaces | **SCAFFOLDED** | `workspaces`/`files` entities exist; `WorkspaceScreen` is wired. No project↔conversation↔artifact association |
| 6 | Library / artifacts | **PARTIALLY_VERIFIED** | Physical artifact upload, hashing, and storage verified via tools/unreal-worker protocol test. Still missing canonical cross-surface UI |
| 7 | Agents & workers | **DISCONNECTED → now PARTIAL** | Provider layer connected this pass. No agent teams, no worker memory, no coordination |
| 8 | Durable execution | **BLOCKED_BY_EXTERNAL_DEPENDENCY** | `HatchetWorkflowProvider` is now reachable and probes for real. No Hatchet runtime exists to talk to |
| 9 | Capability acquisition | **PARTIAL** | `PhysicalRuntimeDiscovery` does real I/O. `CapabilityAcquisitionManager` is DISCONNECTED |
| 10 | Model provider fabric | **PARTIAL** | 5 real providers (Gemini, OpenRouter, Anthropic, OpenAI-compatible, Ollama) are connected. `LiteLLMModelProvider` now reachable, no gateway running |
| 11 | Browser / computer use | **BLOCKED_BY_EXTERNAL_DEPENDENCY** | `PlaywrightBrowserProvider` reachable and probing; no Playwright service |
| 12 | Live observability | **PARTIAL** | `ObservatoryScreen` (1,262 LOC) is wired to the control plane. No OpenTelemetry, no log streaming |
| 13 | Remote execution fabric | **PARTIALLY_VERIFIED** | Control plane worker endpoints (`/enroll`, `/artifacts`) and Node.js Unreal worker implement physical transport. Unreal execution remains pending. |
| 14 | Code development ecosystem | **MISSING** | No editor, terminal, diff viewer, LSP or AST tooling |
| 15–16 | Game dev / interoperability | **MISSING** | No engine adapters of any kind |
| 17 | Research acquisition | **DISCONNECTED** | Entire `multimodal` package unreachable |
| 18–19 | Media studio / previews | **MISSING** | No implementation |
| 20 | Memory completion | **PARTIALLY_VERIFIED** | Levels 0, 1, 5 live and verified. Levels 2, 4, 6 absent |
| 21 | Ontology federation | **IMPLEMENTED_UNVERIFIED** | Now constructed and reachable with its four epistemic categories; no claims flow through it yet |
| 22 | Geospatial opportunity | **PARTIAL** | Location entities + repository connected; `GeospatialSymbolicEngine` DISCONNECTED |
| 23 | Security & owner control | **PARTIAL** | Governance endpoints real and verified. **No authentication on the control plane API at all** |
| 24 | PWA/Android/cloud unification | **PARTIAL** | PWA and control plane verified end to end. Android and PWA share no identity, projects or ledger |
| 34 | Capability catalog | **REAL_AND_CONNECTED** | Built and tested this pass |

---

## Honest position

Of the directive's 44 sections, this pass materially advanced **7, 9, 10, 11, 25
and 34**, and produced the measurement that makes the rest addressable.

Sections 14–19 (code IDE, game fabric, emulation lab, media studio, previews) have
**no implementation to audit**. Federation makes them far cheaper than building
from scratch, but each still needs an adapter, a workspace model, artifact
provenance and a preview path. They are not "nearly done".

The most valuable next move is not another subsystem. It is **wiring the memory
package**, because Sections 4, 20 and 21 all sit on top of it and none of them can
be built honestly while the Level 0 ledger cannot execute.
