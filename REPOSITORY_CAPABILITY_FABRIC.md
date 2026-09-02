# REPOSITORY CAPABILITY FABRIC

What exists across the Owner's repositories, so that M. Engine can answer
*"another project already contains a working implementation of this"* without the
Owner having to remember where it is.

Measured by metadata clone (`--filter=tree:0 --no-checkout`) and tree inspection.
Nothing here has been built or run. Machine-readable twin:
`repository_capability_fabric.json`.

## Inventory

| Repository | Vis | Files | Stack | What it actually contains | Federation value |
| --- | --- | ---: | --- | --- | --- |
| `mhvnsnt/Bannon` | public | — | TS + C++ + UE | The wrestling game. 672 `BannonCore` + 31 `BannonEngine` C++ files, `native/` law headers, a 46k-line single-file web engine | **the target** |
| `mhvnsnt/M.-Engine-` | public | — | Kotlin/Compose + Ktor + React | Governor. Canonical ledger, capability fabric, control plane, PWA | **the governor** |
| `mhvnsnt/bolt.diy-M` | public | 538 | Remix + TS | Fork of bolt.diy: `webcontainer`, `runtime/action-runner`, `message-parser`, `workbench`, `editor`, `git`, `deploy`, **22 LLM providers** | **HIGH — see below** |
| `mhvnsnt/Wrestli6game-3` | public | 48 | React + Vite + TS | A second wrestling game with a **modular** engine: `fighter.ts` 74KB, `character3d.ts` 49KB, `physics`, `referee`/`referee3d`, `moveLibrary`, `sovereign`, `roster.ts` 64KB; components incl. `CreationSuite`, `Customizer`, `TournamentMode`, `PromoScreen`, `MatchRating` | **HIGH — see below** |
| `mhvnsnt/M-Hero-Simulator-` | public | 39 | TS + cjs | Small AI-Studio-style app; carries the same throwaway `fix_*.cjs` / `patch_*.cjs` pattern M. Engine had | low |
| `mhvnsnt/UnrealEngine` | private | — | UE 5.8 | Epic fork. **Source of Lyra** (`Samples/Games/Lyra`) | acquired — see `BANNON_LYRA_FULL_INTEGRATION_MATRIX.md` |
| `mhvnsnt/CODEDUMMY` | private | **1,999** | TS + React + C + Python | **ATTACHED AND AUDITED — see below** | **HIGH** |
| `mhvnsnt/CODEDUMMY-` | private | — | ? | second CODEDUMMY | PENDING_ATTACH |
| `mhvnsnt/God-Mode-OS-D3MN-V2` | private | **519** | TS + React + Python | **ATTACHED AND AUDITED — see below** | **HIGHEST** |
| `mhvnsnt/God-Mode-OS`, `-D3MN` | private | — | ? | earlier generations of the same system | PENDING_ATTACH |
| `mhvnsnt/URBAN-MAYHEM-` | private | — | ? | Another game | PENDING_ATTACH |
| `mhvnsnt/Dream-Infinite-World` | private | — | ? | Unknown | PENDING_ATTACH |

Private repositories are not reachable by anonymous clone. Attaching one is a
single `add_repo` + inline clone each; they are listed so the next pass starts
from a list, and are marked `PENDING_ATTACH` rather than described from guesswork.

---

## The two findings that change M. Engine's build plan

### 1. `bolt.diy-M` is the code-IDE capability M. Engine records as MISSING

`M_ENGINE_REALITY_VERIFICATION_REPORT.md` states plainly: *"Everything in
directive Sections 14–19 (code IDE, game fabric, interoperability lab, media
studio, previews) has no implementation to verify."*

The Owner already has a fork of a mature system that implements most of it:

| M. Engine gap | bolt.diy component |
| --- | --- |
| code IDE | `app/components/editor`, `app/components/workbench` |
| sandboxed execution | `app/lib/webcontainer`, `app/lib/runtime/action-runner.ts` |
| repository mutation | `app/lib/runtime/message-parser.ts` (the write/diff protocol), `app/components/git` |
| model routing | `app/lib/modules/llm/providers` — **22 providers**: amazon-bedrock, anthropic, cerebras, cohere, deepseek, fireworks, github, google, groq, huggingface, hyperbolic, **lmstudio**, mistral, moonshot, **ollama**, open-router, openai, openai-like, perplexity, together, xai, z-ai |
| deploy | `app/components/deploy` |
| persistence / stores | `app/lib/persistence`, `app/lib/stores` |

**`ollama` and `lmstudio` are already there** — the exact local runtimes Bannon's
`CLAUDE.md` names for the God Mode OS (Ollama `localhost:11434`, LM Studio
`localhost:1234`). M. Engine's `LiteLLMModelProvider` and this provider set solve
the same problem; the fabric should ADAPT one of them, not grow a third.

**Recommended integration mode: FEDERATE, not absorb.** bolt.diy is a Remix web
app. M. Engine is Android + Ktor + a React PWA. The honest boundary is a
`CapabilityProvider` in the existing fabric pointing at a running bolt.diy
instance — the same shape as `OpenHandsCodingProvider` and
`PlaywrightBrowserProvider` — so it reports `AVAILABLE` only when one actually
answers. That is one adapter, not an IDE rewrite.

### 2. `Wrestli6game-3` is the modular structure Bannon's web layer lacks

Bannon's web engine is one ~46,000-line HTML file. `Wrestli6game-3` is the same
domain, decomposed:

```
engine/  fighter.ts 74KB   character3d.ts 49KB   physics.ts 14KB
         moves.ts 9KB      moveLibrary.ts 7KB    audio.ts 9KB
         referee.ts 7KB    effects.ts 12KB       sovereign.ts 2KB
data/    roster.ts 64KB    moves.ts 7KB
components/ CreationSuite, Customizer, TournamentMode, PromoScreen,
            MatchRating, MatchReport, ArenaSelection, ControllerUI, TouchUI
```

**This is not a merge candidate.** Bannon's engine is far more advanced (measured
elsewhere: 20 wrestling systems, 5–114 Unreal files each; zoning, pins, reach,
tag classification, MDickie surfacing). The value is the **decomposition**: a
worked example, in the same domain and language, of the module boundaries Bannon's
single file does not have. Read it when splitting `BANNON_v150.html`, and check
`CreationSuite` / `TournamentMode` / `MatchRating` for systems Bannon may not
have — but verify against Bannon's own before importing anything.

---

## CODEDUMMY — attached and audited

`668aca6`, 1,999 files, 245 MB. 372 `.ts`, 278 `.md`, 212 `.tsx`, 143 `.cjs`,
112 `.h`, 97 `.c`, 66 `.py`. Bannon's `CLAUDE.md` names it as the other half of
the two-agent team, and it is the largest capability store outside the two main
repositories.

| Component | Size | What it is | Classification |
| --- | ---: | --- | --- |
| `blendforge/` | 4 files | **A real BullMQ + Redis worker running headless Blender in Docker.** `worker.js` consumes a `blender-pipeline` queue, takes `{blendFilePath, outputGlbPath}` and converts. Dockerfile builds headless Blender 4.x on python:3.10-slim. | **FEDERATE** |
| `godmode/` | **524** | The God Mode OS as a React/Vite app: `daemon/`, `vault/`, `training_data/`, `database/projectMigrations.ts`, `engine/`, `physics/`, `server/`, plus `BANNON_SWARM_BUILDER_v50_1.html` and a `compile-bridge.js`. Matches the description in Bannon's `CLAUDE.md` (daemon, app, EvolutionDaemon, swarm, vault/RAG). | **AUDIT FURTHER** — likely overlaps M. Engine's governor |
| root `*.ts` modules | 21 | A **second independent decomposition** of Bannon's domain: `KinematicCore`, `PhysicsCollider`, `CombatAI`, `MatchDirector`, `CharacterForge`, `CharacterModelGen`, `FighterEvolution`, `SpatialEnvironment`, `InputMatrix`, `FXRenderer`, `AudioSynth`, `Cosmology`, `CloudPersistence`, `DaemonCore`, plus `server.ts` and `start-autonomous-agent.ts` | **PRESERVE AS REFERENCE** |
| `canon/` | 16 | **The story bible** — six book manuscripts plus character files (`edwin_kennedy`, `stan_combs`, `free_agents_roster`, `finxsse_match_notes`). Names that appear in Bannon's live roster. Narrative source, not a capability. | **PRESERVE AS REFERENCE** |
| `box3d-0.1.0/` | 209 C | A vendored 3D physics library | UNAUDITED |
| `blender-mcp-main/`, `tools/blender/` | — | Blender MCP integration; `convert.py`, `extract_anim.py` | **FEDERATE** with `blendforge` |
| `harness-main/` | 35 | Third-party Claude plugin marketplace clone | NOT RELEVANT |
| `memory/` | 2 | Two markdown logs. **Not a memory system** — the name is misleading. | NOT RELEVANT |
| `BANNON_v150.html` | 32,467 lines | An **older, smaller** copy — Bannon's own is 60,064 lines. CODEDUMMY is not the source of truth for the game. | DEPRECATE DUPLICATION |

### What this changes

**1. `blendforge` is the asset-conversion worker the Unreal content pipeline needs.**
Bannon's model pipeline (`ingest_character.sh`, `transfer_weights.cjs`,
`optimize_gltf.cjs`) runs as local scripts. `blendforge` is the same class of work
already packaged as a queued, containerised worker. It is the natural second
member of the worker fabric alongside `tools/unreal-worker`, and it is the piece
that turns "GLB normalization" from a manual step into a fabric capability.

**2. There are now TWO independent decompositions of Bannon's domain** —
CODEDUMMY's root modules and `Wrestli6game-3`'s `engine/`. Neither is a merge
candidate; together they are two worked examples of where the module boundaries
fall, which is worth more than one when splitting a 60k-line file.

**3. `canon/` is Project/Library material.** Six books and character notes that
the Bannon roster is derived from. M. Engine's Project authority and Library
artifact graph are the right home for it — it is exactly the kind of owner
context that should be retrievable rather than sitting in a repository nobody
searches.

**4. `memory/` is a naming trap.** It contains two markdown logs. Anything
planning to "reuse CODEDUMMY's memory system" would find nothing there. Recorded
so the mistake is not made from the directory name — the same class of error as
`mdickie_bases/` in Bannon.

---

## God-Mode-OS-D3MN-V2 — attached and audited

`3b9bdf4`, 519 files, 45 MB, package name `god-mode-nexus`. 268 `.ts`, 143
`.tsx`, 18 `.py`. **76 runtime dependencies.** Entry points are real and named in
`package.json`: `tsx server.ts` (dev) and a build that bundles both `server.ts`
and `orchestrator-core.ts`.

This is the richest capability store in the ecosystem, and it overlaps M. Engine
more than anything else audited.

| Component | What it actually is | Classification |
| --- | --- | --- |
| `src/server/memory/persistentVault.ts` | **A real local vector store.** `sqlite-vec` `vec0(embedding float[768])` virtual table, `EmbeddingEngine.embed()` → `Float32Array`, insert into `vec_memory`, query by vector. | **ADAPT — see the dimension finding** |
| `src/server/memory/pineconeNexus.ts` | Thin cached Pinecone client, LRU-wrapped. Returns `null` with no API key, throws rather than pretending. Honest. | **FEDERATE** (remote, key-gated) |
| `src/server/memory/RedisStateStore.ts`, `lruCache.ts` | Shared state + caching | PRESERVE AS REFERENCE |
| `daemon/` (16 Python) | `swarm_orchestrator`, `daemon_executor`, `daemon_sandbox`, `docker_test_sandbox`, `daemon_graph_engine`, `daemon_telemetry`, `self_healing_loop`, `pd_torque_controller`, `vulkan_wrapper`, `index_bannon` | **AUDIT FURTHER** per daemon |
| `daemon/daemon_sandbox.py`, `docker_test_sandbox.py` + `dockerode` | Containerised execution | **FEDERATE** |
| `daemon/daemon_blender.py` | A second Blender path | **DEPRECATE DUPLICATION** — `blendforge` is the better-packaged one (queue + container) |
| `daemon/daemon_retrieval.py` | **TF-IDF + cosine over workspace files.** See the naming trap below. | PRESERVE AS REFERENCE |
| deps: `@langchain/langgraph`, `@modelcontextprotocol/sdk` | Agent orchestration + MCP | **AUDIT FURTHER** |
| deps: `@mediapipe/pose` | Pose capture — overlaps Bannon's `video_to_clip` | PRESERVE AS REFERENCE |
| deps: `@codesandbox/sandpack-react` | In-browser IDE — overlaps bolt.diy | **DEPRECATE DUPLICATION** (bolt.diy is the richer one) |

### THE FINDING: M. Engine's Memory Level 4 has an implementation to adapt — but NOT to copy

`M_ENGINE_REALITY_VERIFICATION_REPORT.md` records Level 4 (semantic retrieval) as
not existing. `persistentVault.ts` is a working instance of exactly that shape.
But the two do not fit as-is, and the mismatch is silent:

| | M. Engine | God-Mode-OS |
| --- | --- | --- |
| Embedding model | `all-MiniLM-L6-v2.onnx` | (its own `EmbeddingEngine`) |
| **Dimensions** | **384** (`FloatArray(384)`) | **768** (`vec0(embedding float[768])`) |
| Vector index | none | `sqlite-vec` virtual table |
| Store | Room + SQLCipher | `better-sqlite3` |

**384 ≠ 768.** Adopting the vault's schema unchanged would produce a table that
silently never matches anything M. Engine embeds. And `sqlite-vec` is a C
extension that would have to be built for Android, on top of SQLCipher.

So the classification is **ADAPT THE ARCHITECTURE, NOT THE CODE**: embed →
vector table → similarity query, with the dimension taken from M. Engine's own
model. M. Engine already has the embedding half (`EmbeddingEngine`,
`MemoryFragment`, `GraphNode`, `ContextReconstructionEngine`) — Level 4's absence
is not "no embeddings", it is **no retrieval index over the canonical ledger**.

### A fabrication-shaped fallback, recorded so it is not copied

`persistentVault.ts` falls back to a `DummyStmt` when `better-sqlite3` is
missing, whose `run()` returns `{ changes: 1, lastInsertRowid: Date.now() }` and
whose `all()` returns `[]`. **A write that failed reports one row changed.** That
is a silent success on a store that does not exist — precisely what
`REALITY_CONTRACT.md` §7 rejects. If this architecture is adapted, the degrade
path must report `UNAVAILABLE`, not fake a receipt.

### The second naming trap in two repositories

`daemon_retrieval.py` documents itself as matching *"semantic criteria via cosine
similarity"*. It is **TF-IDF** — lexical term statistics, not semantics. Cosine
similarity over TF-IDF vectors finds shared words, not shared meaning. Useful,
and not what the docstring claims. Recorded beside CODEDUMMY's `memory/`
(two markdown files) because both would mislead anyone classifying from names.

---

## Rules

1. **Federate capabilities; do not merge repositories.** Extract a shared library
   only where architecture and licensing both permit.
2. A repository appears here only after its tree was actually read. `PENDING_ATTACH`
   means exactly that — not attached, not described.
3. Every capability claim names the files it was measured from.
