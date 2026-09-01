# BANNON EXECUTION READINESS MATRIX

Generated 2026-09-01. Every row is measured from the repository or explicitly
marked as unverifiable in this environment.

**Purpose:** establish what stands between Bannon and one physically verified
Unreal build, so effort goes at the real blocker instead of at C++ that may not
be the problem.

---

## Verified facts

| Fact | Evidence |
| --- | --- |
| A substantial Unreal C++ module exists | 706 `.cpp`/`.h` files under `unreal/` |
| It targets UE 5.3 | `Bannon.uproject` → `"EngineAssociation": "5.3"` |
| The build config is real, not a stub | `BannonCore.Build.cs`: C++20, Chaos, ChaosCore, PhysicsCore, ControlRig, RigVM, AnimGraphRuntime, EnhancedInput, UMG |
| Native laws are shared, not duplicated | `Build.cs` adds `../../../native/include` so the same header-only combat/physics laws compile for both the web build and Unreal |
| Third-party deps are properly federated | 4 pinned git submodules (JoltPhysics ×2, GGPO, llama.cpp). **Verified by initialising one**: 1,251 files, `Jolt/Jolt.h` present, include path resolves |
| **There is no Unreal content layer** | **0 `.uasset`, 0 `.umap`, 0 `.uplugin`** among 8,260 tracked files. Not gitignored; no `.gitattributes`, so no LFS; no `Content/` directory anywhere |
| The project tracks its own state honestly | `unreal/CONVERSION.md` marks systems `[x]`/`[~]`/`[ ]` and repeatedly states *"Needs the engine to test."* |

---

## The single real blocker

**Bannon has gameplay code and no game.**

Everything Unreal needs in order to *show* something — maps, Blueprints,
Animation Blueprints, skeletal mesh assets, physics assets, blend spaces,
montages, a GameMode, a PlayerController — is absent. The 448 GLB and 202 FBX
files are raw assets the **web** engine loads; none have been imported into
Unreal.

This reframes the reported symptoms. "Models don't animate" and "movesets don't
trigger" are being investigated as bugs in animation code. In an Unreal project
with **no Animation Blueprints and no imported skeletons**, there is no animation
system to be buggy. The C++ may be entirely correct and still produce nothing on
screen.

**Debugging that C++ further, before a content layer exists, is likely to be
wasted effort.** That is the strongest practical finding in this matrix.

---

## Readiness by layer

| Layer | State | Blocker |
| --- | --- | --- |
| Engine association (5.3) | DECLARED | — |
| Module definition | **IMPLEMENTED** | — |
| Third-party dependencies | **IMPLEMENTED** | `git submodule update --init --recursive` |
| Shared native laws | **IMPLEMENTED** | — |
| C++ gameplay code | **IMPLEMENTED_UNVERIFIED** | Never compiled against a real engine |
| **Content: maps** | **MISSING** | No `.umap` |
| **Content: Blueprints** | **MISSING** | No `.uasset` |
| **Content: Animation Blueprints** | **MISSING** | No `.uasset` |
| **Content: skeletal meshes / skeletons** | **MISSING** | FBX/GLB never imported |
| **Content: physics assets** | **MISSING** | No `.uasset` |
| GameMode / PlayerController wiring | **UNVERIFIED** | Requires editor |
| Touch input / mobile controls | **UNVERIFIED** | Requires editor |
| Android packaging | **UNVERIFIED** | Requires engine + Android toolchain |
| Physical device run | **NOT ATTEMPTED** | — |

---

## What cannot be done in M. Engine's current environment

| Requirement | Status |
| --- | --- |
| Unreal Engine 5.3 installation | **CAPABILITY_GAP** — no toolchain reachable; UE is licence-gated and ~100 GB |
| `UnrealBuildTool` | CAPABILITY_GAP |
| Unreal Editor | CAPABILITY_GAP |
| Android + Unreal packaging | CAPABILITY_GAP |
| Physical Android device | CAPABILITY_GAP |

**No Unreal work is claimed as done, and no Unreal adapter was written this
pass.** Writing one would reproduce exactly the failure this whole effort exists
to eliminate: a class that compiles, is never called, and cannot be tested
because its runtime does not exist.

The honest federation mode is **REMOTE_WORKER**: the phone governs and observes;
a machine with UE 5.3 installed does the work. `CapabilityFabric` is already the
right place to register it — it would probe a real Unreal build worker and report
`BLOCKED_BY_EXTERNAL_DEPENDENCY` until one exists, which is today's true state.

---

## Milestone 1 — the only success condition that matters next

Not "classes compile". Not "adapters exist". This chain, observed:

```
Bannon.uproject opens in UE 5.3
  → a map loads
  → a player pawn spawns
  → possession succeeds
  → a Bannon skeletal mesh renders
  → the camera works
  → input moves the character
  → collision behaves
  → an animation visibly plays
  → the physics asset behaves
  → Android package succeeds
  → APK installs and runs on a physical device
```

**Every step needs a machine with the engine. None of it can be established
here.** What *can* be prepared here — and has been — is the audit that says
where to point that machine.

---

## On the Lyra question

Lyra's value is overwhelmingly its **content**: maps, Blueprints, Animation
Blueprints, GAS setup, input config, UI. That is precisely the layer Bannon is
missing, which is why "start from a working Lyra baseline" is a sounder instinct
than "adopt Lyra patterns into the C++".

But two cautions the evidence supports:

1. **Lyra will not supply wrestling.** Grapples, throws, reversals, pins,
   submissions, rope interaction, entrances and the creation suite are Bannon's
   own and exist nowhere in Lyra. Lyra can supply a working *organism* —
   character, movement, camera, input, animation graph, gameplay framework — so
   those systems stop being built on absent infrastructure.

2. **Bannon's shared-`native/` design is an asset worth protecting.** One
   header-only source of truth compiling for both the web build and Unreal is
   genuinely good architecture. A Lyra-derived reconstruction must keep
   `native/include` as the law layer rather than dissolving it into Lyra's
   framework.

Sequencing that follows from the evidence:

```
1. git submodule update --init --recursive      (one command)
2. stand up a machine with UE 5.3               (owner action, unavoidable)
3. compile BannonCore ONCE                      (first real signal)
4. THEN decide Lyra baseline vs pattern adoption
5. build the content layer
6. only then debug animation/moveset symptoms
```

Step 3 is the first point at which any claim about Bannon's C++ becomes
verifiable. Everything before it is preparation; everything after it is
evidence.
