# BANNON REALITY BASELINE

Generated 2026-09-01 from **direct inspection of the repository**. Every count is
measured. Nothing here is inferred from names, documentation or intent.

Engine evidence is absent by necessity: no Unreal toolchain has been reached.
Rows requiring the engine are marked and not guessed at.

---

## 1. Native / shared architecture

| Item | Measured |
| --- | --- |
| C++/headers under `unreal/` | 706 |
| `Source/BannonCore` `.h`/`.cpp` | 672 |
| `Source/BannonEngine` `.h`/`.cpp` | 31 |
| `UCLASS` declarations | 354 |
| Third-party submodules | 4, pinned (JoltPhysics ×2, GGPO, llama.cpp) |
| Shared law layer | `Build.cs` adds `../../../native/include` — the same header-only combat/physics laws compile for both the web build and Unreal |

The shared-`native/` design is the strongest thing in this repository: one source
of truth for the rules, two renderers. **It should be preserved through any
reconstruction, not dissolved into another framework's conventions.**

---

## 2. Two blockers found by inspection

### 2.1 `BannonEngine` is not a module Unreal will ever build

```
Source/BannonCore     672 files   Build.cs present   declared in .uproject
Source/BannonEngine    31 files   NO Build.cs        NOT in .uproject Modules
```

`Bannon.uproject` declares exactly one module (`BannonCore`). `Source/BannonEngine`
has 31 source files, no `Build.cs`, and no entry in the Modules array — so
UnrealBuildTool never sees it. Those 31 files are dead from the engine's point of
view.

**Decide deliberately:** promote it to a real module (add `BannonEngine.Build.cs`
and a Modules entry) or fold its contents into `BannonCore`. Leaving it is the
one option that guarantees confusion at the first compile.

### 2.2 There is no Unreal content layer

| Asset kind | Count |
| --- | ---: |
| `.uasset` | **0** |
| `.umap` | **0** |
| `.uplugin` | **0** |
| `Content/` directory | **absent** |

Re-verified against the ways this could be a false reading: `Content/` is not
gitignored, there is no `.gitattributes` (so no Git LFS), and 0 of 8,260 tracked
files match. **Independently confirmed by the Unreal worker's `inspectContent`
operation**, run against the real checkout:

```json
{"counts":{"uasset":0,"umap":0,"uplugin":0,"fbx":0},
 "evidence":"no Unreal content: project cannot render or animate regardless of C++ correctness"}
```

The 448 `.glb` and 202 `.fbx` files in the repository are raw assets the **web**
engine loads. None have been imported into Unreal.

---

## 3. What the C++ already provides

Measured from actual class declarations:

| Layer | Present | Classes |
| --- | --- | --- |
| Characters | **YES** | `ABannonCharacter`, `ABannonFighter`, `ABannonFighterCharacter`, `ABannonCrowdAgent`, `ABannonReferee` |
| Game modes | **YES** | `ABannonGameMode`, `ABannonMatchManager` |
| Animation instance | **YES** | `UBannonAnimInstance` (1) |
| Actor components | **YES** | 160 |
| `UObject` classes | **YES** | 147 |
| **PlayerController** | **NO** | 0 subclasses of `APlayerController` |
| **Enhanced Input** | **NO** | declared in `Build.cs`, referenced in **0 files** |
| **Gameplay Ability System** | **NO** | 0 files reference `UAbilitySystemComponent`, `UGameplayAbility` or `FGameplayTag` |

`DefaultEngine.ini` sets `GlobalDefaultGameMode=/Script/BannonCore.BannonGameMode`,
and that class does exist — so the configuration points at something real.

---

## 4. What this means for the Lyra decision

The gap is **not** "Bannon has bad gameplay code". It is:

- no content to render,
- no PlayerController,
- no input wiring (the dependency is declared but unused),
- no ability system.

Those four are precisely what a Lyra baseline supplies, and precisely what Bannon
would otherwise have to build from nothing. That is the strongest evidence-based
argument for the Lyra-first approach.

Equally, the evidence bounds what Lyra can do:

| Bannon has | Lyra does not supply |
| --- | --- |
| 5 character classes, 2 game modes, 160 components, an anim instance | wrestling: grapples, holds, chain wrestling, reversals, pins, rope interaction, entrances, creation suite, movesets |

**Lyra supplies a working organism. It does not supply the sport.**

One caution the evidence raises: adopting Lyra means adopting GAS, and Bannon
currently has *zero* GAS. That is not a migration of an existing ability system —
it is introducing one. Budget for it as new work, and keep the `native/` law
layer as the authority for combat rules rather than re-expressing those rules as
Gameplay Abilities.

---

## 5. State summary

| Item | State |
| --- | --- |
| Third-party dependencies | **IMPLEMENTED** — pinned submodules; `git submodule update --init --recursive` |
| Shared native law layer | **IMPLEMENTED** |
| C++ gameplay classes | **IMPLEMENTED_UNVERIFIED** — never compiled against a real engine |
| `BannonEngine` module | **DISCONNECTED** — not declared, not buildable |
| Unreal content layer | **MISSING** |
| PlayerController / input / GAS | **MISSING** |
| Compilation | **UNVERIFIED** — no engine reached |
| Android packaging | **CAPABILITY_GAP** |
| Physical device run | **NOT ATTEMPTED** |

---

## 6. The next physical step

Nothing above required an engine. Everything below requires one.

```bash
# on a machine with UE 5.3
git submodule update --init --recursive
node tools/unreal-worker/server.js --port 8770 --projects /path/to/Bannon
```

Then, in order:

1. `POST /op/probe` — expect `UNREAL_RUNTIME_DISCOVERED: VERIFIED` with a version.
2. `POST /op/build` — **the first real signal.** Capture the compiler output
   whether it passes or fails; the log is the evidence either way.
3. Resolve `BannonEngine`'s module status against whatever the compiler says.
4. Only then decide the Lyra baseline shape, and build the content layer.
5. Only after a character visibly spawns and animates should the reported
   animation/moveset symptoms be investigated — until then there is no animation
   system for them to be symptoms of.
