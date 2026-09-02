# Bannon Architecture: Lyra Baseline Adoption Strategy

**Date:** 2026-09-01
**Objective:** Use Lyra as a proven Unreal infrastructure baseline to repair and replace missing infrastructure layers in Bannon, without overwriting Bannon’s custom wrestling-specific mechanics.

## 1. Architectural Philosophy

The target architecture is a composite of Lyra infrastructure and Bannon sports-specific simulation:

**Lyra/Unreal Infrastructure**
+ **Bannon Core Architecture** (e.g. `BannonCore` framework)
+ **Bannon Wrestling Engine** (`BannonEngine` module)
+ **Bannon Content Layer** (JAGER assets, custom animations, rings)
= **Working Bannon Game**

Bannon retains full responsibility over all wrestling-specific systems (grapples, reversals, chain wrestling, strikes, pins, submissions, rope interactions, entrances, creation systems, and movesets).

## 2. Lyra Capability Classification

| Lyra Capability | Adoption Plan | Reasoning / Application |
| :--- | :--- | :--- |
| **PlayerController** | **ADOPT DIRECTLY** | Bannon currently lacks a proper `PlayerController`. Lyra’s `ALyraPlayerController` handles connection, UI, and input robustly. |
| **Enhanced Input** | **ADAPT** | Lyra uses Enhanced Input heavily. Bannon will map its specialized actions (Grapple, Reversal, Strike) into Lyra’s Input Action/Mapping Context structure. |
| **Character Possession** | **ADOPT DIRECTLY** | Lyra's `ALyraPlayerState` and pawn extension components provide a stable framework for possessing/unpossessing characters seamlessly. |
| **Movement** | **USE AS REFERENCE** | Lyra uses standard `UCharacterMovementComponent`. Bannon must preserve its unique ground-game and ring-boundary logic, possibly subclassing the movement component. |
| **Camera** | **ADAPT** | Lyra’s `ULyraCameraMode` is excellent, but wrestling requires multi-target tracking (framing both fighters) rather than standard third-person follow. |
| **Animation Framework** | **USE AS REFERENCE** | Lyra's animation layers and distance-matching are excellent for shooters but don't natively solve multi-actor synchronized grapples. We will reference its state machine structure. |
| **Gameplay Ability System (GAS)** | **NOT RELEVANT** | **CRITICAL:** Bannon relies on its shared `native/include` combat laws which are engine-agnostic and compile to web/C++. Adopting GAS would force a complete rewrite of combat rules into Unreal's proprietary framework, breaking the cross-engine core. Do not use GAS for combat logic. |
| **Gameplay Tags** | **ADAPT** | Bannon can use Gameplay Tags for state tracking (e.g., `State.Grappled`, `State.Stunned`) to cleanly communicate state between the native combat laws and Unreal UI/Animation. |
| **Health / Damage** | **ALREADY BETTER IN BANNON** | Bannon’s `BannonInjuryManager` and native rules handle limb damage, stamina, and psychology specifically for wrestling. Lyra’s simple health/armor is insufficient. |
| **Networking / Replication** | **ADAPT** | Bannon uses GGPO (Rollback) via pinned submodules. Lyra's standard replication is prediction-based but not rollback. Bannon's rollback will override Lyra's standard movement replication for fighters. |
| **UI Framework** | **ADOPT DIRECTLY** | Lyra’s CommonUI integration, primary game layout, and widget instantiation are production-ready. Bannon will adopt this for menus, HUD, and creation suite structure. |
| **Game Modes / Experiences** | **ADOPT DIRECTLY** | Lyra’s "Experience" system (hot-swapping rules/UI) perfectly fits Bannon's Match Types (Single, Tag, Cage). We will define match types as Lyra Experiences. |
| **Asset / Content Organization** | **ADOPT DIRECTLY** | Bannon currently lacks a `Content/` folder structure. Lyra’s strict separation of core data vs. cosmetic data will be adopted for organizing JAGER and future assets. |

## 3. Next Steps for Implementation

1. **Base Framework Injection:** Introduce Lyra's core GameInstance, GameMode, and PlayerController classes into `BannonCore`.
2. **Experience Definition:** Define the first `BannonExperience` (1v1 Match).
3. **Input Wiring:** Map Bannon's existing `BannonFighterCharacter` actions via Enhanced Input to call functions on the native law layer.
4. **UI Setup:** Stand up a basic CommonUI primary layout to replace the missing HUD.
