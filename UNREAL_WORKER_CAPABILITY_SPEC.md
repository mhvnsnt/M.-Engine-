# UNREAL WORKER CAPABILITY SPEC

Generated 2026-09-01.

Defines how Unreal Engine is federated into M. Engine, and what each capability
state is allowed to mean.

---

## Why remote, and not embedded

Unreal is licence-gated and ~100 GB. It cannot live on the phone, and it cannot
live in M. Engine's container. Any "Unreal integration" that claims otherwise is
a class that compiles, is never called, and cannot be tested — the exact failure
this architecture exists to eliminate.

The federation mode is therefore **REMOTE_WORKER**:

```
Phone / PWA / control plane          governs + observes
        │
        ▼
M. Engine Governor
        │
        ▼
Capability Fabric ── probes ──▶ Unreal Remote Worker   (owner's machine)
                                        │
                                        ├── Unreal Engine 5.3+
                                        ├── build / automation tests
                                        ├── content inspection
                                        └── evidence: logs, exit codes
```

---

## Capability states

Probed, never configured. Evidence is required for every state above
`CAPABILITY_GAP`.

| Capability | How it is established |
| --- | --- |
| `UNREAL_RUNTIME_DISCOVERED` | Locate `UnrealEditor-Cmd` **and execute `-Version`**. Presence alone is `PARTIALLY_VERIFIED`; a successful execution is `VERIFIED` |
| `UNREAL_BUILD_CAPABLE` | Engine build script exists under the discovered engine root |
| `UNREAL_PROJECT_AVAILABLE` | `.uproject` files found under the allowed roots, with `EngineAssociation` and whether a `Content` directory exists |
| `ANDROID_TOOLCHAIN_AVAILABLE` | SDK **and** NDK paths resolve **and** `java -version` executes. All three, or `CAPABILITY_GAP` naming what is missing |
| `PHYSICAL_DEVICE_AVAILABLE` | `adb devices` lists at least one authorised device. `adb` existing is not sufficient |

### The distinction that matters

| Situation | State | Why |
| --- | --- | --- |
| No worker reachable | `UNAVAILABLE` | Nothing to ask |
| Worker up, **no engine** | `PARTIALLY_VERIFIED` | "The worker is up" ≠ "Unreal can build" |
| Worker up, engine version verified | `AVAILABLE` | A command actually ran and returned a version |

Collapsing rows 2 and 3 into one green light is how a capability display stops
being worth reading.

---

## Operations

Allowlisted named functions. **No endpoint accepts a command string.**

| Operation | Purpose | Bound |
| --- | --- | --- |
| `probe` | Re-establish all capability states | seconds |
| `inspectContent` | Count `.uasset` / `.umap` / `.uplugin` in a project | seconds |
| `build` | Compile a project — the first point at which a claim about its C++ becomes verifiable | 45 min |
| `automationTest` | Headless Unreal automation tests | 30 min |

Path confinement, timeouts and evidence-on-failure apply to all of them.

---

## Current status

| Item | State | Evidence |
| --- | --- | --- |
| Worker implementation | **VERIFIED** | Runs; probes correctly on a host with no engine; refuses path traversal; has no arbitrary-exec endpoint |
| `inspectContent` against real Bannon | **VERIFIED** | Returned `0 uasset / 0 umap`, independently confirming the audit |
| Fabric provider + registration | **VERIFIED** | `UnrealWorkerProviderTest` 5/5 against a real HTTP server, fixtures key-matched to real worker output |
| Unreal build execution | **IMPLEMENTED_UNVERIFIED** | No engine has ever been reached. The `build` path has never run against a real Unreal installation |
| Android packaging | **CAPABILITY_GAP** | No toolchain reached |
| Physical device run | **NOT ATTEMPTED** | — |

**Nothing here claims Unreal works.** It claims the boundary works, and that the
boundary reports the truth when Unreal is absent — which is currently always.

---

## What unblocks the next real step

One machine with UE 5.3 installed, running:

```bash
node tools/unreal-worker/server.js --port 8770 --projects /path/to/Bannon
```

At that point `UNREAL_RUNTIME_DISCOVERED` flips to `VERIFIED` with a real version
string, and `build` becomes the first operation that can produce genuine evidence
about Bannon's C++. Everything before that is preparation; everything after is
measurement.
