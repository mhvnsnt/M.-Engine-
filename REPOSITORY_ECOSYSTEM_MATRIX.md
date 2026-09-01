# REPOSITORY ECOSYSTEM MATRIX

Generated 2026-09-01. Revised the same day to correct a dependency finding — see
the correction under Bannon.

Classification is **measured from repository contents**, not from repository
names or descriptions.

---

## Method

`mhvnsnt/Bannon` was cloned and inspected directly: file-type census, engine
marker counts (`.uproject`, `.uplugin`, `.uasset`, `.umap`, `*.Build.cs`),
third-party vendoring, and the project's own conversion tracker.

Other repositories in the account were **not** inspected in this pass and are
therefore listed as UNAUDITED rather than guessed at. Attaching and auditing
each is a bounded, repeatable job — the method above is the template.

---

## Bannon — audited

**Headline: Bannon is not currently a buildable Unreal project, and it is also
not "just a web game". It is a shared-C++-core project with two front ends, one
of which is complete and one of which has never been compiled.**

| Measure | Value |
| --- | ---: |
| `.uproject` | 1 (`unreal/Bannon.uproject`, EngineAssociation 5.3) |
| `*.Build.cs` | 1 (`BannonCore`) |
| C++/headers under `unreal/` | **706 files** |
| `.uasset` | **0** |
| `.umap` | **0** |
| `.uplugin` | 0 |
| `.glb` models | 448 |
| `.fbx` animations | 202 |
| TypeScript / TSX | 499 / 318 |
| C++ / headers (whole repo) | 458 / 551 |

### What this means

1. **There is a real Unreal C++ module.** `BannonCore.Build.cs` is not a stub —
   it targets C++20, depends on Chaos, ControlRig, RigVM, EnhancedInput and
   AnimGraphRuntime, and deliberately adds `native/include` so the same
   header-only combat and physics laws compile for both the web build and
   Unreal. That is a genuinely good architecture: one source of truth for the
   rules, two renderers.

2. **There is no Unreal content whatsoever.** Zero `.uasset`, zero `.umap`. No
   Blueprints, no maps, no Animation Blueprints, no imported skeletal meshes.
   The 448 GLB and 202 FBX files are raw assets that the *web* engine loads;
   nothing has been imported into Unreal.

3. **CORRECTION — the dependencies are fine.** An earlier revision of this
   document stated that `BannonCore` could not compile because
   `unreal/ThirdParty/{JoltPhysics,GGPO,llama.cpp}` were empty. **That was
   wrong, and the error was mine.**

   All three are **git submodules**, correctly declared in `.gitmodules` with
   pinned upstream commits:

   ```
   160000 commit 072675b1…  unreal/ThirdParty/JoltPhysics   -> jrouwe/JoltPhysics
   160000 commit 7ddadef8…  unreal/ThirdParty/GGPO          -> pond3r/ggpo
   160000 commit 736ffea4…  unreal/ThirdParty/llama.cpp     -> ggerganov/llama.cpp
   160000 commit 765845d6…  native/third_party/JoltPhysics  -> jrouwe/JoltPhysics
   ```

   They appeared empty only because this audit used a `--depth 1` clone and
   never ran `git submodule update --init`. **Verified by actually doing it:**
   initialising JoltPhysics fetched 1,251 files and produced
   `unreal/ThirdParty/JoltPhysics/Jolt/Jolt.h` — the exact include path
   `Build.cs` adds now resolves.

   This is not a defect. Pinned submodules against upstream repositories are
   precisely the federation practice the open-source directive asks for: the
   dependency is governed and version-locked without its source tree being
   absorbed into the project.

   **The one-line remedy for anyone cloning Bannon:**
   `git submodule update --init --recursive`

   *Lesson worth keeping: a shallow clone is not the repository. An audit
   instrument that omits submodules will report correctly-federated
   dependencies as missing.*

4. **The project already tracks its own state honestly.** `unreal/CONVERSION.md`
   marks systems `[x]` landed / `[~]` laws ready / `[ ]` not started, and says
   plainly of the ragdoll and floating-capsule movement: *"Needs the engine to
   test."* That is the correct posture and matches what this audit found.

### Consequence for the Lyra question

Lyra Starter Game is a **content-heavy** Unreal project: its value is largely in
Blueprints, input configuration, GAS setup, maps and UI assets. Bannon has no
content layer at all.

So "integrate Lyra into Bannon" cannot mean merging assets — there is nothing to
merge them into. The only honest options are:

| Option | Assessment |
| --- | --- |
| **Reference architecture** (adopt GAS / modular gameplay *patterns* into `BannonCore`) | **Recommended.** Matches what Bannon already is: a C++ module. No content dependency |
| Lyra-derived new project, Bannon systems ported in | Viable but a rewrite; Bannon's shared-`native/` design would have to be re-established |
| Copy Lyra source into `unreal/Source` | **Not recommended.** Bannon's module is deliberately thin over `native/`; Lyra's framework assumes its own content and would fight that |

**Prerequisite for all three: one successful `BannonCore` compile on a machine
with UE 5.3.** The dependencies are already in place (see correction above); what
is missing is an engine to compile against. Until a single successful compile
exists, any statement about Lyra integration is unverifiable.

---

## Unreal / Lyra capability status

| Capability | State | Evidence |
| --- | --- | --- |
| Unreal toolchain in M. Engine's execution environment | **CAPABILITY_GAP** | No `UnrealBuildTool`, `UnrealEditor` or engine installation reachable. Unreal is license-gated and ~100 GB; it cannot live in this container |
| `BannonCore` compiles | **UNVERIFIED** | Dependencies resolve once submodules are initialised; no UE 5.3 toolchain exists here to compile against |
| Lyra federation | **NOT STARTED** | Correctly not attempted — see below |

**Nothing about Unreal or Lyra is claimed as implemented or verified in this
pass.** Writing an Unreal adapter here would produce exactly the pattern this
whole effort exists to eliminate: a client class that compiles, is never called,
and cannot be tested because the runtime does not exist.

The architecturally correct shape — and the owner's own stated preference — is
that the phone **governs and observes** while a machine with the engine
installed does the heavyweight work. That makes Unreal a **REMOTE_WORKER**
federation mode, not an embedded dependency. The `CapabilityFabric` built in the
previous pass is already the right place to register it: it would probe a real
Unreal build worker and report `BLOCKED_BY_EXTERNAL_DEPENDENCY` until one exists
— which is the honest state today.

---

## Other repositories — UNAUDITED

Listed for completeness. None were inspected in this pass; no claim is made
about their contents.

| Repository | State |
| --- | --- |
| `mhvnsnt/M.-Engine-` | Audited continuously — see `M_ENGINE_COMPLETENESS_MATRIX.md` |
| `mhvnsnt/Bannon` | **Audited above** |
| `mhvnsnt/UnrealEngine` | UNAUDITED (fork; likely the engine source itself) |
| `mhvnsnt/Wrestli6game-3` | UNAUDITED |
| `mhvnsnt/Dream-Infinite-World` | UNAUDITED |
| `mhvnsnt/M-Hero-Simulator-` | UNAUDITED |
| `mhvnsnt/bolt.diy-M` | UNAUDITED |

---

## Federation rule this matrix enforces

A repository provides capabilities to M. Engine **without being merged into
it.** Integration modes: `DEPENDENCY`, `GIT_SUBMODULE`, `GIT_SUBTREE`,
`MAINTAINED_FORK`, `EXTERNAL_SERVICE`, `LOCAL_RUNTIME`, `REMOTE_WORKER`,
`OPTIONAL_CONNECTOR`, `REFERENCE_ONLY`.

Cloning large upstream projects into the M. Engine repository is explicitly not
the plan. M. Engine governs; it does not absorb.
