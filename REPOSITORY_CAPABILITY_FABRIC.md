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
| `mhvnsnt/CODEDUMMY`, `CODEDUMMY-` | private | — | ? | Named in Bannon's `CLAUDE.md` as the **other half of the two-agent team** (God-Mode OS side) | **PENDING_ATTACH — likely high** |
| `mhvnsnt/God-Mode-OS`, `-D3MN`, `-D3MN-V2` | private | — | ? | The God Mode OS referenced throughout Bannon's `CLAUDE.md`; `BANNON_GODMODE` is its in-game front end | **PENDING_ATTACH — likely high** |
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

## Rules

1. **Federate capabilities; do not merge repositories.** Extract a shared library
   only where architecture and licensing both permit.
2. A repository appears here only after its tree was actually read. `PENDING_ATTACH`
   means exactly that — not attached, not described.
3. Every capability claim names the files it was measured from.
