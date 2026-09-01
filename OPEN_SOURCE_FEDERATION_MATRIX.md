# OPEN SOURCE FEDERATION MATRIX

Generated 2026-09-01.

Governs which mature open-source systems M. Engine federates rather than
rebuilds. M. Engine stays the Canonical Governor; these are specialised organs.

**Verification state uses `REALITY_CONTRACT.md` vocabulary.** "Adapter connected"
means the client is reachable from a running entry point and probes the real
backend — it does **not** mean the backend exists.

---

## Licence is a shipping constraint, not a footnote

One entry below is a genuine commercial blocker and is called out first so it is
not discovered late:

> **MinIO is GNU AGPL v3.** MinIO completed its transition to AGPLv3 in
> `RELEASE.2021-05-11T23-27-41Z`; the server, client and gateway are all AGPLv3.
> Its vendor states that any commercial or proprietary usage is at your own risk
> and must be validated against AGPLv3 obligations, which can extend to releasing
> your own source. A commercial licence is sold for exactly this case.
>
> **Consequence:** if M. Engine is ever distributed as a closed-source product,
> MinIO cannot be embedded or shipped with it without a commercial licence.
> Running it as a *separate self-hosted service the owner operates* is a very
> different posture from bundling it, and is the only mode assumed here.
>
> Permissive S3-compatible alternatives (Apache-2.0 / BSD) should be evaluated
> before any decision to bundle object storage.

---

## The federated set

| Capability | Selected project | Licence | Integration mode | Verification state | Why |
| --- | --- | --- | --- | --- | --- |
| Coding agent | **OpenHands** | MIT | Remote service, HTTP | **Adapter connected, backend absent** — `BLOCKED_BY_EXTERNAL_DEPENDENCY` | Mature autonomous SWE agent with a documented REST API; rebuilding it is the single largest avoidable cost in the directive |
| Durable workflow | **Hatchet** | MIT | Remote service, HTTP | Adapter connected, backend absent | Postgres-backed durable task queue; replaces hand-built retry/lease plumbing |
| Model gateway | **LiteLLM** | MIT | Remote service, HTTP | Adapter connected, backend absent | One OpenAI-compatible surface over 100+ providers; collapses the per-provider fan-out |
| Browser / computer use | **Playwright** | Apache-2.0 | Remote service, HTTP | Adapter connected, backend absent | The mature automation baseline; already proven in this repo's own PWA test harness |
| Artifact storage | **MinIO** | **AGPL-3.0** ⚠ | Self-hosted service only | Adapter connected, backend absent | S3-compatible object storage. **See licence warning above** |
| Canonical database | **PostgreSQL** | PostgreSQL Licence (permissive) | Remote service | Adapter connected, backend absent | Already the control plane's production ledger option |
| Semantic retrieval | **pgvector** | PostgreSQL Licence | Postgres extension | **NOT INTEGRATED** | Rides the Postgres decision; no vector store exists yet |
| Sandbox execution | *M. Engine native* | in-repo | In-process | **REAL_AND_CONNECTED** | The only provider needing no external runtime |
| Game engine build | **Unreal Engine 5.3+** | Epic EULA (not open source) | **REMOTE_WORKER** via `tools/unreal-worker` | **Boundary VERIFIED, engine CAPABILITY_GAP** | Licence-gated and ~100 GB: it cannot live on a phone or in a container. The worker is written and proven; no engine has been reached |
| Game gameplay foundation | **Lyra Starter Game** | Epic EULA, owner-authorised | REFERENCE / project baseline | **NOT STARTED** | Content-heavy; useful precisely because Bannon lacks a content layer. Audit before adopting |

### Evaluated and deliberately deferred

| Capability | Candidates | Decision |
| --- | --- | --- |
| Telemetry | OpenTelemetry (Apache-2.0) | Deferred. The right substrate, but there is no worker fleet emitting spans yet — instrumenting an empty fabric measures nothing. The Unreal worker is the first real worker; revisit once a second exists |
| Code editor | Monaco (MIT), CodeMirror (MIT) | Deferred to §14. Monaco is the VS Code editor core and is the obvious pick; it needs a workspace model first |
| Coding agents (alt) | Aider (Apache-2.0) | Not selected. Excellent CLI-driven agent, but process-oriented rather than service-oriented — OpenHands' HTTP API is the better federation boundary |
| Workflow (alt) | Temporal (MIT) | Not selected. More mature than Hatchet but materially heavier to self-host; revisit if Hatchet proves insufficient |
| 3D / media | Blender (GPL-2.0+), FFmpeg (LGPL/GPL) | Deferred to §18. Both are *tools invoked as processes*, not libraries to link — which keeps their copyleft at arm's length. That distinction must be preserved |

---

## Embedding modes — be aggressive about federation, selective about embedding

Owner correction, recorded because it prevents a predictable failure: pulling
"everything possible" into the source tree produces dependency hell, licence
conflicts and partial copies of fifty unrelated applications. The rule is
aggressive *federation*, selective *embedding*.

| System | Mode | Never |
| --- | --- | --- |
| Unreal / Lyra | source/project on the **Unreal worker** | not in the Android app |
| Blender | external application **worker** | not embedded |
| OpenHands | **service / worker** | not vendored |
| Hatchet or Temporal | durable control-plane **service** | not reimplemented |
| Playwright | **browser worker** | not in-app |
| Ghidra | authorised-analysis **worker** | not in the normal coding loop |
| FFmpeg | **media worker** capability | process, not linked library — which also keeps its copyleft at arm's length |
| Postgres + pgvector | canonical server-side **persistence/retrieval** | not a second authority |
| OpenTelemetry | observability **substrate** | not before workers exist to emit spans |
| ComfyUI | optional generative-media **worker** | not copied into the source tree |

M. Engine governs. It does not absorb.

---

## The rule this matrix enforces

Do not pull a repository merely because it is open source. Each entry required:

1. a real capability gap in the measured matrix,
2. a licence compatible with the intended distribution mode,
3. an integration boundary M. Engine can probe and verify,
4. an honest state when the backend is absent.

Point 4 is why every adapter here reports `BLOCKED_BY_EXTERNAL_DEPENDENCY` rather
than degrading into a simulation. A federated system that fakes availability when
it is missing is worse than no federation at all — it launders a guess into the
evidence ledger.

---

## Current honest status

Seven providers are registered and probed on every entry to the Capability Fabric
screen. **One is available: the in-process native sandbox.** The other six have no
running backend in any environment M. Engine currently reaches, and say so, with
the endpoint that failed.

That is not a failure of the federation work. It is the correct reading, and it
doubles as the install list: standing up any one of these backends flips its row
to `REAL_AND_CONNECTED` with no code change.

Sources: [MinIO AGPLv3 announcement](https://www.min.io/blog/from-open-source-to-free-and-open-source-minio-is-now-fully-licensed-under-gnu-agplv3), [minio/minio LICENSE](https://github.com/minio/minio/blob/master/LICENSE), [MinIO commercial licence](https://www.min.io/commercial-license), [OpenHands Cloud API docs](https://docs.openhands.dev/usage/cloud/cloud-api)
