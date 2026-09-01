# Mission 17.1H — Persistent Autonomous Runtime

## Core Invariant
M. Engine must not rely solely on the owner having the UI open to function. It must maintain a persistent, autonomous metabolism that survives application restarts, backgrounding, and idle periods. "Continuous monitoring" is not real until a physical scheduler exists and is verified.

## 1. The Autonomous Metabolism
We transition from synchronous, UI-driven inspections to an asynchronous, time-and-event-driven background loop. The metabolism answers the question: *"When M. Engine is not open on your screen, what exactly keeps it alive?"*

## 2. Physical Background Execution (WorkManager)
In the Android environment, this is achieved through `WorkManager`, guaranteeing that:
- Work survives process death and device reboots.
- Work respects system constraints (e.g., `NetworkType.CONNECTED`).
- Work is scheduled periodically (the "heartbeat").

## 3. The Waking Cycle
When the `EcologyMetabolismWorker` wakes up in the background:
1. **Observe**: It checks the active project registry.
2. **Re-evaluate**: It retrieves the latest SHAs for tracked projects.
3. **Change Detection**: It pipes any detected deltas into the `ChangeDetectionEngine` (from 17.1G).
4. **Plan**: It uses the `ReinspectionPlanner` to decide if it should expend resources to fully inspect the new commits.
5. **Execute**: It performs targeted updates, emitting `EvidenceOfAction`.
6. **Sleep**: It yields back to the OS until the next interval.

## 4. Observatory Logging
Background cycles emit their activity stream to the Agency Observatory, preserving the requirement to show operational reality (e.g., `OBSERVED: Repository HEAD changed`, `INTENT: Targeted manifest comparison`) without exposing fake internal monologues.
