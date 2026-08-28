# Phase 17: M. Engine Web + Shared Control Plane

## Physical Boundaries & Current Execution Environment
M. Engine is currently executing within a cloud-based Android build environment (generating APKs for a streaming emulator). This environment physically restricts the deployment of a standalone Web/PWA client (e.g., Next.js, Node.js server, or Compose for Web) since the preview and build tooling are strictly wired for Android APK execution.

In adherence to the **REALITY_CONTRACT.md**:
*   **Missing Dependency**: A general-purpose web server or browser execution environment for previewing the PWA.
*   **Actual Boundary**: We have isolated the `MissionEngine`, `PersonalContextEngine`, and `EvidenceEngine` into a headless **Shared Control Plane**. The Android APK now acts strictly as a client to this backend.
*   **No Mocks**: We are not presenting a simulated HTML file or mock web server. The Android Room DB has been upgraded to persist Mission States so that any future Web/PWA client connecting to the shared data layer will find Missions intact after process termination or disconnects.

## Shared Control Plane Architecture
```text
                 M. ENGINE SHARED CORE
                          │
        ┌─────────────────┼─────────────────┐
        ▼                 ▼                 ▼
     Memory           Research          Missions
        │                 │                 │
        └─────────────────┼─────────────────┘
                          ▼
                 Worker Orchestrator
```

## Durable Mission State
Missions are now stored in `MissionEntity` via `MissionDao`. A client disconnect (closing the Android app or browser tab) does not destroy the Mission's authoritative state. When the client reconnects, it queries the shared database to resume the `UniversalRealityLoop`.

## Delegated Auth & CI
Manual secrets are officially an exceptional fallback. For Firebase App Distribution and CI, we now use **Google Cloud Workload Identity Federation** (OIDC) rather than long-lived service account keys, eliminating another manual credential step.
