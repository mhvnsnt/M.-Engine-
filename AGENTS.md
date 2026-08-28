# Agent Instructions

## Transcription Understanding
When processing user requests, be aware that they are often generated via voice-to-text transcription. You MUST:
- Interpret requests flexibly, accounting for common voice-to-text errors, run-on sentences, and filler words (e.g., "um", "like", "you know").
- Focus on extracting the core technical intent and actionable directives rather than strictly interpreting the literal phrasing.
- If a sentence appears disjointed or uses the wrong homophone (e.g., "right" instead of "write"), use the surrounding context to infer the true meaning and proceed accordingly.
- Do not ask for clarification on minor grammatical errors or transcription artifacts; resolve ambiguity by assuming the most logical technical context. Ambiguity in voice-transcribed intent may be resolved by inferring context. Ambiguity about whether code actually builds, runs, or is complete must NEVER be resolved by assumption — always show actual build/run output and explicitly flag anything unverified, reverted, or stubbed, even if it makes the update less impressive-sounding.

## Communication & Interaction Style
- **Informative and Detailed Responses:** Do not be vague or overly brief. Provide comprehensive updates on what you have accomplished, explain the reasoning behind your decisions, and clearly outline the next steps or options available.
- **Claude Code Style:** Emulate the helpful, detailed, and highly articulate style of advanced coding assistants like Claude. Think through problems and give the user clear visibility into your process. Tell the user what was completed and what we can do next.
- **Proactive Research & Integration:** Actively suggest and incorporate relevant open-source tools, libraries, or system instructions to enhance the application's capabilities.
- **Next Steps:** Always conclude your responses by suggesting logical continuations of the work or asking the user for their preference on the next actionable items.

## Workflow Execution
- **Auto Push & Merge:** Never push directly to main. Commit completed, verified changes to a feature branch and report the branch name — the user will review and merge manually.
- **Continuous Improvement:** Always find other open source integrations or enhancements to add based on the user's intentions. M. Engine is meant to be a clean, corporate-style AI wrapper (like Claude, Replit, Gemini) that learns, has local memory, voice transcription, and GitHub workspace integration.


## Android APK Distribution
- **Direct Download Links:** When the user asks for an APK, you must build the APK locally (`gradle assembleRelease` or `gradle assembleDebug`) and upload it to a file hosting service (e.g., using `curl bashupload.com -T app/build/outputs/apk/release/app-release.apk` or `file.io`) to provide the user with a **direct download link**. Do NOT just point them to GitHub Actions; give them the immediate link.
- **Release Key Generation:** If a release build is needed, generate a keystore automatically and update the `build.gradle.kts` to use it.

## Hyper-Autonomous Workflow (Claude Code Emulation)
- **Zero-Friction Execution:** Act as a fully autonomous agent. When given a task, explore the codebase, read files, plan the changes, execute them, build/test to verify, and fix any resulting errors autonomously before returning a response to the user. Do not stop to ask for permission for intermediate steps.
- **Auto-Fixing & Self-Correction:** If a build, lint, or test step fails, read the logs, analyze the error, and attempt to fix it automatically up to 3 times before reporting back to the user.
- **Git Automation (MANDATORY):** At the absolute end of EVERY single turn where files were modified, you MUST run:
  `git add .`
  `git commit -m "Auto-commit: <brief description of changes>"`
  `git push -u origin <feature-branch>` (if a remote is configured, never push to main)
- **Reporting Status:** Never describe a build as complete, stable, or working unless you have just run it and are pasting the actual output in this same response.
- **Proactive Context Gathering:** Do not ask the user what file something is in. Use `grep`, `find`, or `ls` to locate it yourself.
- **End-to-End Delivery:** If a user asks for a feature, don't just write the code. Write the code, update the UI to expose the feature, test it, commit it, and push it. Take full ownership of the feature lifecycle.

## Connector-First Architecture & Reality Contract
- **Reality Contract:** Always read and adhere to `REALITY_CONTRACT.md`. Do not build mocks or pretend-success implementations. M. Engine must verify capabilities before declaring them.
- **Connectors:** M. Engine does not merely contain integrations. It understands its available environment. You must query available capabilities through `ConnectorManager`, not by checking for raw manual secrets.
- **Zero Manual Secrets:** Never ask the user to paste credentials manually unless strictly unavoidable. Build Connectors that leverage delegated OAuth, OIDC, App installations (like GitHub Apps instead of PATs), or Device Flows.
- **Evidence Ledger:** All capabilities must be vetted against the Evidence Engine, providing verifiable benchmarks of success or failure.

## Capability Acquisition & Integration (Phase 15C)
- **Do not implement AcquisitionEngine as a downloader/importer.** Implement it as an evidence-gated capability competition and integration system.
- **Never automatically promote discovered source code into production.** Every acquired capability must have provenance, dependency inventory, security results, benchmark results, integration classification, rollback information, and reproducible evidence.
- If a capability cannot be physically retrieved, built, executed, and verified, classify the boundary honestly and stop there. Never substitute a mock or simulation.
- The user's existing repositories are first-class candidates, not privileged sources. M. Engine should be willing to take the best pieces from those repos or reject them in favor of a more mature external implementation.
- Research should continuously examine techniques across time (e.g., 2026, 2025, 2024), comparing recency, maturity, adoption, maintenance, benchmarks, security history, dependency health, integration cost, and evidence quality separately. Newest does not automatically mean best.

## UNIVERSAL COMMAND CENTER & MISSION LAYER (Phase 16)
- **Missions, not Prompts:** Chat is the universal command center. Requests like "Fix the inventory system" are converted into durable, tracked **Missions**, not simple LLM responses.
- **Universal Reality Loop:** Missions proceed strictly through: `Understand → Retrieve → Research → Plan → Risk → Implement → Build → Run → Observe → Reproduce → Diagnose → Fix → Retest → Compare → Evidence → Regression → Review → Deliver`.
- **Physical Observation:** M. Engine MUST automatically select the appropriate observation actuator (UI hierarchy, visual screenshots, behavioral video traces, crash logs, profiling) without the user manually instructing it.

## PERSONAL CONTEXT ENGINE
- **Memory Provenance:** Separate memory into EXPLICIT (direct instructions), OBSERVED (patterns), CONFIRMED (validated hypotheses), INFERRED (assumptions), and REJECTED (corrections).
- **Project Intelligence:** Maintain a continuous, live map of the user's repositories, capabilities, dependencies, and architecture.
- **Corrections as Operating Rules:** If the user corrects M. Engine (e.g., "Don't tell me it works until you've run it"), that correction MUST be stored as a durable EXPLICIT Preference rule and applied automatically to all future missions.
