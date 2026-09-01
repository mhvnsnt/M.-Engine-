# M. ENGINE CANONICAL ARCHITECTURE

Generated 2026-09-01. Reflects **measured** repository state, not intent.

Every component below carries its real status. A component marked SCAFFOLD or
DISCONNECTED is part of the intended architecture and is *not* currently part of
the running organism — saying so is the point of this document.

---

## Canonical data ownership

Every important data class has exactly one authority. Indexes, projections and
UI caches are never authorities.

| Data | Canonical authority | Status |
| --- | --- | --- |
| Raw conversation + operational history | `ImmutableConversationLedger` → `RoomConversationLedger` | **CANONICAL, VERIFIED** |
| UI message list | Room `messages` table | **PROJECTION** (read model, not authority) |
| Owner goals & terminology preferences | Room `owner_goals`, `terminology_preferences` → `OwnerContextGraph` | **CANONICAL, VERIFIED** |
| Ontology claims + epistemic category | `OntologyFederationEngine` | CONNECTED, in-memory only |
| Task-scoped working context | `ContextReconstructionEngine` | **CONNECTED, VERIFIED** |
| Capability availability | Live probe via `CapabilityFabric` | **CONNECTED, VERIFIED** (never cached) |
| Project state | — | **MISSING** |
| Artifacts / Library | — | **MISSING** |
| Evidence graph | `EvidenceEngine` | CONNECTED, not on a promotion path |
| Knowledge claims | — | **SCAFFOLD** |
| Secrets | Android keystore / settings | PARTIAL — DB key is hardcoded (see Risks) |

---

## The memory hierarchy, as actually built

```
            owner message / M. Engine response
                          │
                 ChatRepository.insertMessage        ← single funnel
                          │
              ┌───────────┴────────────┐
              ▼                        ▼
     Room `messages`         RoomConversationLedger
     (UI projection)              LEVEL 0  ✔ VERIFIED
                                       │
                          ┌────────────┼────────────┐
                          ▼            ▼            ▼
                   OwnerContext   Project mem   Knowledge
                   LEVEL 1 ✔      LEVEL 2 ✘     LEVEL 3 ✘
                          │        MISSING       SCAFFOLD
                          ▼
                  Semantic retrieval  LEVEL 4 ✘  DISCONNECTED
                          │
                          ▼
              ContextReconstructionEngine LEVEL 5 ✔ VERIFIED
                          │
                          ▼
                 active conversation / worker
                          │
                          ▼
                   Meta-memory  LEVEL 6 ✘  MISSING
```

**Levels 0, 1 and 5 are live and verified. Levels 2, 3, 4 and 6 are not.**

Why the funnel is at `ChatRepository` and not the call sites: `insertMessage` is
invoked from a dozen places in `ChatViewModel`. Appending to the ledger at each
would guarantee one is eventually missed — the recurring failure where a rule is
only as good as the paths that remember to apply it. Intercepting at the single
choke point means no message path can reach storage without also reaching
Level 0.

---

## Capability fabric

```
                    CapabilityFabric  ✔ CONNECTED
                            │  probes concurrently, never caches
   ┌──────────┬─────────────┼─────────────┬──────────┬─────────┐
   ▼          ▼             ▼             ▼          ▼         ▼
OpenHands  Hatchet      LiteLLM      Playwright   MinIO    Postgres
 coding    workflow      models       browser    artifacts    db
   ✘          ✘             ✘             ✘          ✘         ✘
        all BLOCKED_BY_EXTERNAL_DEPENDENCY — no backend running
                            │
                            ▼
                 NativeFallbackProvider  ✔ AVAILABLE (in-process)
```

Adapters are connected and probe for real. **One provider is available.** The
other six honestly report the endpoint that failed, which doubles as the install
list.

---

## Product surfaces

```
   Android (Compose)      PWA (React)        Cloud control plane (Ktor)
   ✔ ledger, memory,      ✔ installable,     ✔ REST + CORS verified
     fabric, drawer         offline shell,     SQLite/Postgres ledger
                            governance
          │                     │                      │
          └─────────────────────┴──────────────────────┘
                                │
                   ✘ NO SHARED IDENTITY / PROJECTS / LEDGER
```

**This is the largest remaining architectural gap.** The three surfaces are
verified individually and share no canonical state. Android holds the memory
pipeline; the PWA talks to the control plane; the control plane has its own
separate agency ledger. Unifying them requires the Project model, which does not
exist yet.

---

## Component classification

**Canonical (authority):** `ImmutableConversationLedger`/`RoomConversationLedger`,
`OwnerContextGraph`, `ContextReconstructionEngine`, `OntologyFederationEngine`,
`CapabilityFabric` probe state.

**Adapters:** `OpenHandsClient`, `HatchetClient`, `LiteLLMClient`,
`PlaywrightClient`, `MinIOClient`, `PostgresClient`, `RemoteControlPlaneRepository`.

**Clients:** Android Compose UI, PWA, `ObservatoryScreen`, `CapabilityFabricScreen`.

**Projections (never authorities):** Room `messages`, PWA IndexedDB, UI caches.

**Scaffold (present, not load-bearing):** `federated/environment`, knowledge
claims, `EvidenceEngine` promotion path.

**Disconnected (77 files, 5,414 LOC):** `ai/capabilities/directed`,
`ai/capabilities/multimodal`, `ai/capabilities/integration`,
`SemanticResearchGraph`, `ResearchHistoryEngine`, `MemoryIndependenceCheck`.

**Deprecation candidates:** `FileBackedConversationLedger` (superseded by
`RoomConversationLedger`; see the deprecation matrix — not removed).

---

## Risks this architecture currently carries

1. **The database encryption key is a hardcoded string literal**
   (`"super-secret-mengine-key"` in `MainActivity`). Anyone with the APK has it,
   so the SQLCipher layer protects against casual file access only. It should
   move to the Android keystore.
2. **`fallbackToDestructiveMigration(true)` is still enabled** as a last resort.
   Declared migrations now run first and preserve data, but any future schema
   version without a declared path will still destroy the ledger. Level 0 and
   destructive fallback are fundamentally in tension.
3. **The control plane API has no authentication** while exposing `pause`,
   `resume` and `emergency_stop`. CORS is allow-listed, which is not an
   authorization boundary.
4. **Levels 2, 4 and 6 are absent**, so "memory" today means Level 0 + owner
   context + reconstruction. It is not yet project memory.
