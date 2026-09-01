# PROTOTYPE DEPRECATION MATRIX

Generated 2026-09-01. Reproduce the reachability column with
`node scripts/audit/reachability.cjs`.

---

## The rule

**Nothing is deleted because a replacement interface exists.** A replacement must
walk the whole path before the old system is retired:

```
IMPLEMENTED → PHYSICALLY CONNECTED → PARTIALLY_VERIFIED → VERIFIED
           → OBSERVED RELIABILITY → old system deprecated
```

No component in this repository has yet reached OBSERVED RELIABILITY, so
**nothing is scheduled for removal in this pass.**

---

## Duplicate authorities

| Component | Reachable | State | Replacement | Action | Safe-removal condition |
| --- | --- | --- | --- | --- | --- |
| Room `messages` table | YES | **DUPLICATE → resolved** | `RoomConversationLedger` | **RECLASSIFIED, not removed.** It is now the UI read model; the ledger is the authority. Both are written through one funnel | Never removed — a projection is legitimate. Removal would only follow the UI reading the ledger directly |
| `FileBackedConversationLedger` | NO (false positive: named only in comments) | **OBSOLETE** | `RoomConversationLedger` | **PRESERVE.** Java-serialized plaintext on disk, versus SQLCipher-encrypted Room. Superseded on both authority and security grounds | Delete only once nothing can construct it and no on-disk ledger file exists in any install |
| `InMemoryConversationLedger` | YES | **KEEP** | — | Legitimate for tests | Not a duplicate authority |

## Fabricated implementations

| Component | State | Action taken |
| --- | --- | --- |
| `OpenHandsWorkerAdapter` (previous revision) | **FABRICATED** — `delay()` + hardcoded returns, including a literal `"BUILD SUCCESSFUL in 2s"` CI result and a fake diff for a fictional `DummyTest.kt`; dispatched to `/sandbox/provision`, an endpoint that does not exist in OpenHands | **REMOVED** in the previous pass. Now delegates to the real client and throws `CapabilityGapException` when no runtime is reachable |
| `LiveCodingRealityOrchestrator` verification step | **SELF-VALIDATING FICTION** — asserted success by matching `"BUILD SUCCESSFUL"` against the string the adapter had just hardcoded | **REMOVED.** Treats runtime events as the only evidence; reports `PARTIALLY_VERIFIED` |
| Hardcoded terminology in `ContextReconstructionEngine` | **HARDCODED OWNER DATA** — the owner's stated preference existed only as a Kotlin literal, changeable solely by recompiling | **REMOVED** this pass. Now hydrated from `terminology_preferences` with provenance and supersession |

## Destructive configuration

| Component | State | Action taken |
| --- | --- | --- |
| `fallbackToDestructiveMigration(true)` | **DATA-DESTROYING** — every schema version bump deleted the entire encrypted database, all conversation history with it. Incompatible with a Level 0 record | **MITIGATED, not removed.** Declared migrations (10→11, 11→12) now run first and are purely additive. The fallback remains only for a database predating any declared path. **Remove entirely once a full migration chain exists** |
| Hardcoded SQLCipher key `"super-secret-mengine-key"` | **INEFFECTIVE ENCRYPTION** — shipped in the APK | **FLAGGED, not changed.** Rotating it without a re-key migration would render existing owner databases unreadable. Must move to the Android keystore *with* a migration path |

## Disconnected subsystems (77 files, 5,414 LOC)

| Package | Files | State | Disposition |
| --- | --- | --- | --- |
| `ai/capabilities/directed` | 10 | DISCONNECTED | **PRESERVE.** Directed autonomy loop; wire after the Project model exists |
| `ai/capabilities/multimodal` | 3 | DISCONNECTED | **PRESERVE.** Belongs to research acquisition |
| `ai/capabilities/integration` | 2 | DISCONNECTED | **PRESERVE.** Audit for overlap with the provider fabric first |
| `SemanticResearchGraph`, `ResearchHistoryEngine`, `ResearchMemoryState` | 3 | DISCONNECTED | **PRESERVE.** These are Level 4 retrieval; they are the natural next wiring target |
| `MemoryIndependenceCheck` | 1 | DISCONNECTED | **PRESERVE.** Memory provenance guard; belongs with Level 6 |
| `CapabilityAcquisitionManager` | 1 | DISCONNECTED | **PRESERVE.** Overlaps `CapabilityFabric`; consolidate rather than duplicate |
| `ui/FabricScreen.kt` | 1 | DISCONNECTED | **REVIEW.** Possible overlap with the new `CapabilityFabricScreen`; compare before either is kept |

## Overlaps to consolidate (not yet resolved)

| Overlap | Note |
| --- | --- |
| `CapabilityFabric` vs `FederatedCapabilityRegistry` (`ecology`) | Two capability registries. The `ecology` one has a failing test asserting `verifiedOperationalCount > 0`. Determine which is canonical before either grows further |
| `ui/FabricScreen` vs `ui/CapabilityFabricScreen` | Two fabric UIs; the older is disconnected |
| Control-plane agency ledger vs Android conversation ledger | Two independent ledgers on two surfaces. Resolving this needs the Project model and a sync boundary |

---

## Nothing removed this pass

Deletions this pass: **fabricated code paths only** — the simulated adapter
methods and the hardcoded owner preference. No working subsystem, no persistence,
no owner data.
