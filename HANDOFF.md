# HANDOFF — M.-Engine-

**Last updated: 2026-11-22 (America/New_York).**
**Repo:** `mhvnsnt/M.-Engine-` (PUBLIC), default branch `main`.
**Purpose of this file:** any agent that picks up work here reads this FIRST, then the linked docs. It states what we've done, where we're at, and what we're working toward — so the owner can say "go to this repo, pick up the work" and you just know. Update this file in the same PR whenever the state changes (add, don't delete).

## 1. What this repo is

**The M. Engine Android Applet.** README: "This repository contains the M. Engine Android Applet. It is configured with autonomous workflows and GitHub Actions to minimize manual intervention." Repo description: "Be like me".

It is a Kotlin/Gradle Android app (`app/`, `library/`, `build.gradle.kts`, `settings.gradle.kts`, `gradlew`) with GitHub Actions CI/CD that automatically builds both Debug and Release APKs on every push to main (APKs uploaded as workflow artifacts), Dependabot checking Gradle and GitHub Actions updates weekly, and a cloud control plane (`cloud_control_plane/`, `m-engine-web/`, `shared-control-plane/`, `functions/`). The repo also carries a large set of mission/canon docs (`MISSION_5.1` through `MISSION_17.2C`, `M_ENGINE_CANONICAL_ARCHITECTURE.md`, `M_ENGINE_COMPLETENESS_MATRIX.md`, `BANNON_EXECUTION_READINESS_MATRIX.md`, `BANNON_REALITY_BASELINE.md`, `CAPABILITY_LEDGER.md`, `REALITY_CONTRACT.md`, and more). Created 2026-08-05 — the oldest repo in this batch. ~5 MB on disk.

## 2. Where we're at

### DONE
- **Applet staged (created 2026-08-05).** The M. Engine Android applet + CI/CD + control plane.
- **Supervisor daemon merged (2026-10-18, PR #22).** "Merge pull request #22 from mhvnsnt/feat/supervisor-daemon" — the supervisor daemon feature.
- **Working method statement (2026-10-27).** "Add working method statement" — the last commit on main. Last push 2026-11-11 (the only repo in this batch whose last push is not 2026-11-20/21 — it went quiet a little earlier).

### IN PROGRESS
- **PR #1 — "build(deps): bump roborazzi from 1.59.0 to 1.73.0"** (open since 2026-08-27, Dependabot). Screenshot-test dependency bump.
- **PR #2 — "chore(deps): bump org.jetbrains.kotlinx:kotlinx-coroutines-test from 1.10.2 to 1.11.0"** (open since 2026-08-27, Dependabot). Coroutines test bump.
- **PR #3 — "chore(deps): bump androidx.room:room-runtime from 2.7.0 to 2.8.4"** (open since 2026-08-27, Dependabot). Room runtime bump.
- **PR #4 — "chore(deps): bump androidx.room:room-ktx from 2.7.0 to 2.8.4"** (open since 2026-08-27, Dependabot). Room KTX bump.
- **PR #5 — "chore(deps): bump androidx.core:core-ktx from 1.18.0 to 1.19.0"** (open since 2026-08-27, Dependabot). Core KTX bump.

### BLOCKED
- **Nothing is stated as blocked.** Five Dependabot PRs have sat open since 2026-08-27 — nearly three months. They are routine dependency bumps, not blockers, but they have never been merged or closed. No code commits since 2026-10-27. The repo reads as paused.

## 3. What we're working toward

**The goal: the M. Engine applet building clean on CI (Debug + Release APKs on every push) with dependencies kept current.** That is what the README promises — autonomous workflows, minimal manual intervention. The immediate work is the five Dependabot bumps: they are small, they are old, and they either merge clean or get superseded by newer Dependabot runs.

**The bar for done here:** the five Dependabot PRs triaged (merge or close with a reason), CI green on main, and this HANDOFF.md kept current. The mission/canon docs are reference material — read them before changing architecture, don't rewrite them in a lane.

## 4. Standing rules that apply here

- **Branch → PR → merge, always.** Never push to main directly. Add-only, zero-conflict PRs, no force pushes.
- **Checkpoint-and-save law:** CHECKPOINT.md first, update after every step; the repo is the save; resume, never restart.
- **No-temp-storage law:** nothing in /tmp or any wipeable location, ever. Everything goes straight into the repo or a durable folder the second it exists.
- **Real-source-only:** every part comes from the actual source code and files, wired up fully. No hand-tooled lookalikes, no fake panels.
- **Never delete game files.** Game files (ISOs, ROMs, game archives, teardown source) are never auto-deleted. They come off local disk only after 100% hash/size-verified storage on GitHub OR TeraBox, retrievability proven, and the deletion logged.
- **No-gaps / solve-it law:** a gap or block is never the end of the work — route-switch until it's done or every reasonable route is exhausted with exact errors recorded.
- **Never kill in-progress work.** Never delete another lane's work without the standing verification rules.

## 5. Key files and paths

| What | Where |
|---|---|
| README (what it is + CI/CD) | `README.md` |
| Android app | `app/`, `library/`, `build.gradle.kts`, `settings.gradle.kts`, `gradlew` |
| CI/CD | `.github/` (workflows build Debug + Release APKs on push to main) |
| Cloud control plane | `cloud_control_plane/`, `m-engine-web/`, `shared-control-plane/`, `functions/` |
| Mission / canon docs | `MISSION_*.md`, `M_ENGINE_CANONICAL_ARCHITECTURE.md`, `M_ENGINE_COMPLETENESS_MATRIX.md`, `M_ENGINE_REALITY_VERIFICATION_REPORT.md`, `BANNON_EXECUTION_READINESS_MATRIX.md`, `BANNON_REALITY_BASELINE.md`, `CAPABILITY_LEDGER.md`, `CAPABILITY_GAP_MATRIX.md`, `REALITY_CONTRACT.md`, `OPEN_SOURCE_FEDERATION_MATRIX.md`, `REPOSITORY_ECOSYSTEM_MATRIX.md` |
| Agent working docs | `AGENTS.md`, `WORKING-METHOD.md` |
| Docs | `docs/` |
| Memory | `memory/` |
| Open work | PRs #1–#5 (all Dependabot bumps, all 2026-08-27) |

## 6. Open decisions needing the owner

- **Is M. Engine live or parked?** No code commits since 2026-10-27, five Dependabot PRs open since August. If it is parked, the bumps can wait; if it is live, they are three months overdue. His call — lanes do not decide the order of work across projects.
- **The five Dependabot PRs (#1–#5):** merge them or close them? They are routine bumps, but a blind merge without a green CI run is a guess. If CI is green, merging is low-risk; if CI is red, the bumps are not the problem to fix first. His call on whether the applet still matters enough to keep current.

## 7. How to pick up

1. Read this HANDOFF.md first, then `README.md`, `AGENTS.md`, and `WORKING-METHOD.md` in the repo.
2. Check CI status on main before touching the Dependabot PRs — a red build changes the job.
3. Triage the five Dependabot PRs (#1–#5) in order; close any that Dependabot has superseded with a newer run.
4. Read the mission/canon docs before changing architecture — don't rewrite them in a lane.
5. Resume from the stated next step — never restart, never re-derive finished work.
6. Land work branch → PR → merge, add-only. Stage finished work to the repo as it lands.
7. Update this HANDOFF.md in the same PR when the state changes. Verify with your own eyes before claiming anything done.
