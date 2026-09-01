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
| Production Kotlin files | 282 |
| Production LOC | 31,041 |
| Top-level declarations | 824 |
| **Reachable from an entry point** | **197 files (69.9%)** |
| **DISCONNECTED** | **85 files, ~5,800 LOC (30.1%)** |
| Files carrying simulation markers | 49 |
| Total simulation markers | 97 |

Roughly **one line in five of this codebase has never been able to execute.**

That is the actual bottleneck, and it is not a shortage of code. It is the gap
between written and wired.

---

## Subsystem table

| Subsystem (package) | Files | LOC | Reachable | Real I/O | Sim markers | Measured state |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| `ai/capabilities` | 69 | 9219 | 49/69 | 13 | 44 | PARTIAL / MARKERS PRESENT |
| `ui` | 20 | 6059 | 19/20 | 2 | 12 | PARTIAL / MARKERS PRESENT |
| `ai/capabilities/ecology` | 40 | 5283 | 18/40 | 6 | 12 | PARTIAL / MARKERS PRESENT |
| `ai` | 16 | 1735 | 15/16 | 2 | 2 | PARTIAL / MARKERS PRESENT |
| `data` | 33 | 1241 | 33/33 | 1 | 0 | CONNECTED (performs real I/O) |
| `ai/cloud` | 7 | 1192 | 7/7 | 3 | 2 | IMPLEMENTED_UNVERIFIED (markers present) |
| `ai/capabilities/federated/provider` | 17 | 906 | 16/17 | 14 | 3 | PARTIAL / MARKERS PRESENT |
| `ai/capabilities/federated/environment` | 9 | 687 | 6/9 | 1 | 3 | PARTIAL / MARKERS PRESENT |
| `ai/capabilities/memory` | 9 | 601 | 0/9 | 1 | 1 | DISCONNECTED |
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
| `ui/theme` | 3 | 142 | 1/3 | 0 | 0 | PARTIALLY CONNECTED |
| `ai/capabilities/integration` | 2 | 124 | 0/2 | 0 | 2 | DISCONNECTED |
| `ai/capabilities/boundary` | 3 | 121 | 1/3 | 0 | 0 | PARTIALLY CONNECTED |
| `ai/capabilities/workspace` | 1 | 109 | 1/1 | 0 | 0 | CONNECTED (in-process only) |
| `other` | 2 | 106 | 1/2 | 0 | 0 | PARTIALLY CONNECTED |
| `ai/capabilities/mutation` | 1 | 78 | 1/1 | 0 | 0 | CONNECTED (in-process only) |
| **TOTAL** | **282** | **31041** | **197/282** | | **97** | |

---

## The finding that matters most

**The `ai/capabilities/memory` package is 100% disconnected — 9 files, 601 LOC.**

It contains, precisely, the systems Section 2 of the completion directive names
as M. Engine's unique intelligence to preserve:

| File | Role in the stated architecture |
| --- | --- |
| `ImmutableConversationLedger.kt` | Level 0 raw record — "must never be replaced by summaries" |
| `FileBackedConversationLedger.kt` | Its persistence |
| `ContextReconstructionEngine.kt` | Context Reconstruction Engine |
| `OwnerContextGraph.kt` | Owner Context Graph |
| `OntologyFederation.kt` | Ontology Federation |
| `SemanticResearchGraph.kt` | Semantic retrieval |
| `ResearchHistoryEngine.kt` | Research memory |
| `ResearchMemoryState.kt` | Memory state model |
| `MemoryIndependenceCheck.kt` | Memory provenance guard |

None of it is reachable from the running application.

Conversations **do** persist — but through a *different* path: Room entities
(`sessions`, `messages`, `memory_fragments`) via `ChatRepository`, which is fully
connected. So there are two parallel memory architectures, and the one that is
live is not the one the architecture documents describe.

This is the highest-value wiring target in the repository, and it is the
precondition for directive Sections 4 (Persistent Conversations) and 20 (Memory
Completion). Neither can be honestly built on top of a ledger that cannot run.

Other fully-disconnected packages:

- `ai/capabilities/directed` — 10 files, 493 LOC (Directed Autonomy, `AutonomousOpportunityLoop`)
- `ai/capabilities/multimodal` — 3 files, 205 LOC (Section 17, research acquisition)
- `ai/capabilities/integration` — 2 files, 124 LOC

---

## Directive sections against measured reality

States use the vocabulary of `REALITY_CONTRACT.md`.

| § | Capability | Measured state | Evidence |
| --- | --- | --- | --- |
| 3 | Universal Sidebar / Home | **PARTIAL** | The drawer is real and routes work. 5 of 10 destinations are literally `Text("… (WIP)")`: Home, Apps, Games, Workspaces, Agents |
| 4 | Persistent conversations | **PARTIAL_REAL_IMPLEMENTATION** | Room `sessions`/`messages` persist. No titles, search, folders, pinning, archival, branching. The Immutable Ledger is DISCONNECTED |
| 5 | Project workspaces | **SCAFFOLDED** | `workspaces`/`files` entities exist; `WorkspaceScreen` is wired. No project↔conversation↔artifact association |
| 6 | Library / artifacts | **MISSING** | Zero classes named `Library`. No artifact store, no provenance, no content hashing |
| 7 | Agents & workers | **DISCONNECTED → now PARTIAL** | Provider layer connected this pass. No agent teams, no worker memory, no coordination |
| 8 | Durable execution | **BLOCKED_BY_EXTERNAL_DEPENDENCY** | `HatchetWorkflowProvider` is now reachable and probes for real. No Hatchet runtime exists to talk to |
| 9 | Capability acquisition | **PARTIAL** | `PhysicalRuntimeDiscovery` does real I/O. `CapabilityAcquisitionManager` is DISCONNECTED |
| 10 | Model provider fabric | **PARTIAL** | 5 real providers (Gemini, OpenRouter, Anthropic, OpenAI-compatible, Ollama) are connected. `LiteLLMModelProvider` now reachable, no gateway running |
| 11 | Browser / computer use | **BLOCKED_BY_EXTERNAL_DEPENDENCY** | `PlaywrightBrowserProvider` reachable and probing; no Playwright service |
| 12 | Live observability | **PARTIAL** | `ObservatoryScreen` (1,262 LOC) is wired to the control plane. No OpenTelemetry, no log streaming |
| 13 | Remote execution fabric | **SCAFFOLDED** | `federated/environment` 6/9 reachable; no registered remote node |
| 14 | Code development ecosystem | **MISSING** | No editor, terminal, diff viewer, LSP or AST tooling |
| 15–16 | Game dev / interoperability | **MISSING** | No engine adapters of any kind |
| 17 | Research acquisition | **DISCONNECTED** | Entire `multimodal` package unreachable |
| 18–19 | Media studio / previews | **MISSING** | No implementation |
| 20 | Memory completion | **DISCONNECTED** | See above. Level 0 ledger unreachable |
| 21 | Ontology federation | **DISCONNECTED** | `OntologyFederation.kt` unreachable. `astro_profiles` entity exists |
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
