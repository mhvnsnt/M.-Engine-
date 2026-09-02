# M. ENGINE REALITY VERIFICATION REPORT

Generated 2026-09-01.

This is the short list: **things physically demonstrated in this pass**, and the
evidence for each. Everything not on this list is not verified, regardless of
what any other document says.

`REALITY_CONTRACT.md` states that "the code compiled" is entirely distinct from
"the capability actually works". This report honours that split.

---

## VERIFIED — demonstrated end to end

### Capability Fabric performs real network probes and reports honestly

`CapabilityFabricTest`, **3/3 passed.**

Deliberately **not** mocked. A mocked probe would prove only that the mock works
— the substitution §7 of the contract rejects. Instead a real `HttpServer` is
bound to a real port and the fabric is pointed at it, so a pass means bytes
crossed a socket.

| Assertion | Result |
| --- | --- |
| A backend answering 200 on `/health` reads `AVAILABLE` / `REAL_AND_CONNECTED` | pass |
| An absent backend reads `UNAVAILABLE` / `BLOCKED_BY_EXTERNAL_DEPENDENCY` | pass |
| An unavailable provider states *why*, naming `CAPABILITY_GAP` | pass |
| A backend killed **between probes** stops reporting `AVAILABLE` | pass |
| `availableProviders()` reflects the measurement, not the registry | pass |
| The in-process native sandbox is available with nothing installed | pass |

The fourth row is the important one: availability is **re-measured, never
remembered**. A cached `AVAILABLE` is exactly the stale status the contract
forbids.


### Canonical memory pipeline — Level 0, 1 and 5 (canonicalization pass)

`CanonicalLedgerPersistenceTest` **3/3** and `CanonicalMemoryHydrationTest`
**3/3**, against a REAL file-backed Room database that is genuinely closed and
reopened. An in-memory database would satisfy a "data is still there" assertion
without proving anything survived.

| Assertion | Result |
| --- | --- |
| Every message reaches Level 0 through the single `ChatRepository` funnel | pass |
| Level 0 survives closing and reopening the persistence layer | pass |
| Provenance (platform, conversation id) survives the restart, not just text | pass |
| A correction supersedes **without deleting** the original event | pass |
| The supersession chain resolves to the active successor | pass |
| Backfill of pre-ledger messages is idempotent — a re-run adds nothing | pass |
| Terminology preference is loaded from storage, absent before hydration | pass |
| Seeding runs once only and never overwrites an owner edit | pass |
| A superseded preference stops being applied but remains on disk | pass |
| Reconstruction excludes superseded events from worker context | pass |
| Reconstruction does not replay the whole ledger (bounded slice) | pass |

**Two real bugs were caught by these tests, not by reading:**

1. Every live message was being written with the event id `msg-0`. Room assigns
   an `autoGenerate` primary key *at insert*; the object handed to the repository
   still carries `id = 0`. Deriving the event id from it collided every message
   onto one row, and the DAO's `IGNORE` conflict strategy dropped the rest
   **silently**. Fixed by using the row id Room returns.
2. A string-escaping error emitted the Kotlin literal `"msg-\$id"` — a literal
   `$id`, identical for every message. Same symptom, different cause.

Both produced a ledger that looked like it worked and stored one event per
session. This is exactly why the restart test asserts counts rather than
existence.

**Android debug APK still builds** after the wiring: `:app:assembleDebug`
BUILD SUCCESSFUL, `app-debug.apk` 54.5 MB.

### Android debug build produces a real, installable APK

Against a real SDK (platform 36, build-tools 36.0.0, NDK 25.1.8937393):

```
:app:validateSigningDebug  BUILD SUCCESSFUL
:app:assembleDebug         BUILD SUCCESSFUL
:app:compileDebugKotlin    BUILD SUCCESSFUL   (after fabric wiring)
```

`app-debug.apk` — 54 MB, package `com.aistudio.mengine.axwz`, minSdk 24.
`apksigner verify` → `Signer #1 DN: C=US, O=Android, CN=Android Debug`.

This is the first APK the repository has produced; all 30 prior CI runs failed.

### PWA ⇄ control plane, over real CORS

Against the Ktor server built from this tree:

- `/health` succeeded from the browser through CORS
- live governance state and mindstream rendered from the real SQLite ledger
- **Pause/Resume mutated real server state** (`autonomyEnabled` true → false → true)
- a disallowed origin received `403`
- 19/19 PWA checks passed on a 412×915 viewport, including app-shell render with
  the network fully offline

---

## PARTIALLY_VERIFIED

**The federated provider layer is reachable and probing.** 16 of 17 files moved
from disconnected to reachable, and the probe path is proven by the test above.
What is *not* verified is any provider doing real work, because no OpenHands,
Hatchet, LiteLLM, Playwright, MinIO or Postgres backend exists in any environment
M. Engine currently reaches.

**OpenHands adapter dispatch.** The client is written against the documented V1
App Conversations API and the fabricated code paths are gone. No dispatch has
ever been executed against a live OpenHands instance. Treat the request/response
mapping as `IMPLEMENTED_UNVERIFIED` until it is.

---

## Fabrications removed this pass

Both were **disconnected**, which is why neither had ever surfaced. That is what
made them dangerous: the first caller to arrive would have received a green
result for work that never ran.

| Site | What it did |
| --- | --- |
| `OpenHandsWorkerAdapter.retrieveTestOutput()` | Returned the literal `"BUILD SUCCESSFUL in 2s\n1 test completed, 0 failed"` — a fabricated CI result, named explicitly as forbidden by the contract |
| `OpenHandsWorkerAdapter.retrieveDiff()` | Returned a hardcoded git diff for a fictional `DummyTest.kt` |
| `OpenHandsWorkerAdapter.*` | Dispatched to `/sandbox/provision`, `/sandbox/{id}/execute`, `/sandbox/{id}/diff` — **endpoints that do not exist in OpenHands** |
| `LiveCodingRealityOrchestrator` | Verified success via `rawTestOutput.contains("BUILD SUCCESSFUL")` against the string the adapter had just hardcoded — the evidence engine validating its own fiction |

The orchestrator now treats runtime events as the only evidence, hashes them to
pin exactly which bytes a verdict rests on, and reports `PARTIALLY_VERIFIED`
rather than claiming operational verification it has not performed. Cleanup is
reported `CLEANUP_UNKNOWN` because the OpenHands API exposes no teardown call it
can inspect.

---

## Known-failing, pre-existing, untouched

`ecology.FederatedCapabilityFabricTest` — **2 failing assertions**, chiefly:

```
AssertionError: Verified operational count must be > 0
```

It references none of the code changed in this pass. The failure is arguably
*correct behaviour*: the test asserts that at least one capability is
`VERIFIED_OPERATIONAL`, and none is. The reality contract is holding; the test was
written on an optimistic assumption.

`m_engine_ci.yml` will still fail: it runs `./gradlew lintDebug detekt`, and
**detekt is not configured anywhere** in the build. `android.yml` is the workflow
that now produces an APK.

---

## Corrected since the last revision

The previous report stated that Sections 4, 20 and 21 rested on a memory package
that was 100% disconnected. **That is no longer true for Levels 0, 1 and 5**,
which are now connected and verified above. It remains true for Levels 2, 4 and
6 (project memory, semantic retrieval, meta-memory), which do not exist.

---

## Not verified, and not claimed

Everything in directive Sections 14–19 (code IDE, game fabric, interoperability
lab, media studio, previews) has no implementation to verify.

**Memory Levels 2, 4 and 6 do not exist**: no project memory, no semantic
retrieval on the canonical path, no meta-memory. "Memory is done" would be an
overstatement — Level 0, Level 1 and Level 5 are done.

**`OntologyFederationEngine` is reachable but no claims flow through it.** Its
epistemic categories exist and are correct; nothing yet produces an
`OntologyClaim` at runtime, so the empirical/symbolic separation is enforced by
construction rather than demonstrated under load.

**No worker has executed against a live federated backend.** All six external
providers remain `BLOCKED_BY_EXTERNAL_DEPENDENCY`.

**The three product surfaces share no canonical state.** Android holds the memory
pipeline, the PWA talks to the control plane, and the control plane keeps its own
separate agency ledger. This is the largest remaining architectural gap and it
needs the Project model, which does not exist.

## Unreal Worker Artifact Transport (Phase 3)
- **Status:** `PARTIALLY_VERIFIED`
- **Result:** Physical byte transport from `worker.js` via HTTP POST (`/artifacts`) to the M. Engine governor has been executed end-to-end using a synthesized test artifact. The file was successfully written, hashed, uploaded, and stored in the canonical `Library` system with proven hash equality. Unreal Engine execution itself remains `IMPLEMENTED_UNVERIFIED` pending physical workstation enrollment.

---

# RECONCILIATION PASS — 2026-09-02

Two agents worked in parallel. This section records what was measured when their
work was brought together, including two corrections to earlier claims made in
this very document.

> **Status: CI wiring and content-address identity have been independently
> verified. End-to-end artifact transport and physical Unreal Engine compilation
> remain pending external runtime evidence.**
>
> Removing a phantom CI failure means the repository is finally testing what it
> genuinely can test at this stage. It does NOT mean the thing that check
> claimed to test now works.

## Confirmed by independent measurement — Google AI Studio's work

**Content-address identity: `VERIFIED`.** `library/artifacts/` contains a
physically stored artifact whose FILENAME IS THE SHA-256 OF ITS CONTENT:

```
sha256(content) = aa4f406646607c08fc45f351bd6b1584d98bfca09e785c631f39546a6af835ae
filename        = aa4f406646607c08fc45f351bd6b1584d98bfca09e785c631f39546a6af835ae
```

Independently recomputed here, not read off a report. Content-addressed storage
with proven hash equality.

**Full artifact transport end to end: `PARTIALLY_VERIFIED`.** The property above
is one link in the chain, not the chain. Nothing has yet carried a real artifact
through the complete Android → control plane → remote worker path, so the
identity scheme is proven while the transport it serves is not.

**Unreal-generated artifacts: `IMPLEMENTED_UNVERIFIED`.** No physical Unreal
worker has generated or returned one.

**The control-plane sync endpoints are real on the server.** `/api/v1/ledger/sync`
and `/api/v1/ledger/events` exist in `ControlPlaneServer.kt` and are backed by a
real SQLite table with `INSERT OR IGNORE` and a `timestamp > since` query. Not a
stub. Kept as written.

## CORRECTION 1 — the Android half of that sync could never run

**State: was `DISCONNECTED`, presented as working. Now `IMPLEMENTED_UNVERIFIED`.**

`RemoteControlPlaneRepository` holds connection state PER INSTANCE, and only
`refreshState()` sets `CONNECTED`. Four instances existed and
`RoomConversationLedger` constructed a private fifth that nothing ever
connected. Both sync calls gate on `CONNECTED`, so both short-circuited forever.
Both call sites then swallowed the outcome (`// Offline or failed`, `// Ignore`).

This is the exact failure the contract exists to catch: **a subsystem that is
silent when it works and silent when it cannot run at all is not observable, and
an unobservable subsystem cannot be called verified.**

Fixed: one shared instance, `NotConnectedException`, and a
`LedgerSyncDiagnostic` StateFlow carrying `NEVER_ATTEMPTED / NOT_CONNECTED /
FAILED / SYNCED`. Guarded by `CanonicalSyncWiringTest`, 6/6.

It is **`IMPLEMENTED_UNVERIFIED`, not `VERIFIED`**: the wiring is now correct and
the outcome is readable, but no Android build has synced against a live control
plane. That demonstration is what would move it, and it has not happened.

Two further defects fixed on the way, both present on BOTH surfaces:

- **The pull cursor raced the device against itself.** It read the newest event
  overall, which every locally authored message pushes past anything the control
  plane still holds — so remote events older than your last message could never
  arrive. The cursor is now the newest event that arrived BY SYNC.
- **The remote push sat on the canonical write path.** `appendSuspending` is the
  single funnel for every message and `backfillLedgerFromMessages` drives it once
  per historical message at launch, so a working sync would have made startup N
  sequential round-trips. Level 0 durability is local and no longer waits on the
  network.

## CORRECTION 2 — "red on two test assertions" was wrong

This document previously attributed `build_and_verify` being red to two
`FederatedCapabilityFabricTest` assertions. **It was red at `detekt`, the FIRST
step, so the test step never ran at all.**

The baseline was generated at `fb84e50`; `ProjectRepository`, `ProjectEntities`
and `ProjectsScreen` arrived afterwards in PR #8, so their findings were new and
correctly rejected. The gate was working; the reading of it was not. 48 findings
→ 0, fixed in code rather than baselined.

A separate regression from the same blind spot: `CapabilityFabricTest` asserted
7 providers while PR #9 registered an eighth. CI never reported it because
detekt failed first.

**Lesson, and it is the same one twice: a red step masks every step behind it.
Read WHICH step failed before attributing the failure.**

## Still not verified, and not claimed

- **No worker has executed against a live federated backend.** Unchanged. Eight
  tests remain red because they require services that do not exist in CI
  (OpenHands, Playwright, a physical worker) or reach `api.github.com`. They are
  honest failures and were deliberately NOT mocked into passing.
- **Unreal execution remains `IMPLEMENTED_UNVERIFIED`** pending physical
  workstation enrollment.
- **Memory Levels 2, 4 and 6 still do not exist.**
- **The three surfaces still do not share canonical state IN PRACTICE.** The
  mechanism now exists and is correctly wired on all three, which is a real
  change from "no mechanism at all" — but a mechanism that has never carried a
  message between two surfaces is not shared state. It is a path that should
  work.

## OBSERVED CI EVIDENCE — run 33588343177, head `b3c76d8`

Recorded from the real GitHub runner, not from a local sandbox.

| Step | Result |
| --- | --- |
| `./gradlew lintDebug detekt` | **BUILD SUCCESSFUL in 5m 56s** |
| `./gradlew testDebugUnitTest` | 164 tests completed, **5 failed** |

**The detekt gate passing on a real runner is the first time this repository's
`build_and_verify` has got past its first step since PR #8.** Before this commit
it died at `./gradlew: Permission denied`, exit 126.

**The sandbox was not the target, and measuring on the target changed the
answer.** Locally 9 tests failed; on the runner 5 did. Four of the local
failures — `FederatedCapabilityFabricTest` (both), `ObservabilityRealityTest`,
`CapabilityLifecyclePersistenceTest` — **pass in CI**. They reach
`api.github.com`, which this sandbox proxies and the runner does not. Reshaping
them against local conditions would have broken working tests.

The 5 real failures:

| Test | Cause | Status |
| --- | --- | --- |
| `ExampleRobolectricTest` | `UnsupportedOperationException` in `DefaultSdkProvider` | **FIXED** — `sdk = [36]` needs JDK 21; CI runs JDK 17. Pinned to 33, matching the other 9 |
| `BrowserAutomationIntegrationTest` | `ConnectException` | Needs a live Playwright service |
| `RemoteCodingDelegationTest` | `ConnectException` | Needs a live OpenHands worker |
| `TaskRoutingIntegrationTest` | `ConnectException` | Needs a live physical worker |
| `LiveCodingRealityTrialTest` | `WORKER_UNREACHABLE` | Needs a live physical worker |

The last four are integration tests against services that do not exist in CI.
They are honest failures and are **deliberately left failing**: mocking them
would manufacture a pass for a capability that does not work, and skipping or
quarantining them would hide it. They are the same fact already recorded above —
**no worker has executed against a live federated backend** — expressed as a red
check instead of a sentence. They go green when a worker is enrolled, not before.
