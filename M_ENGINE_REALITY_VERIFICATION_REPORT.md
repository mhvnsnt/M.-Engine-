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
