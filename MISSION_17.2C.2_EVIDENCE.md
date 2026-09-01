# MISSION 17.2C.2 — REMOTE DEPLOYMENT REALITY BRIDGE EVIDENCE

## Architectural Status
* **IMPLEMENTED**: `AgencyLedgerRepository` interface abstracted to support multiple persistence layers.
* **IMPLEMENTED**: `SQLiteLedgerRepository` preserved for local deterministic testing and safe simulation.
* **IMPLEMENTED**: `PostgresLedgerRepository` created to communicate with the canonical PostgreSQL schema.
* **IMPLEMENTED**: Ktor JVM API scaffold serving `/health`, `/ready`, `/api/v1/mindstream`, and remote kill switch endpoints.
* **IMPLEMENTED**: Environment-variable driven configuration (`AGENCY_DB_TYPE`, `AGENCY_DB_URL`, `PORT`).
* **IMPLEMENTED**: Dockerfile for `cloud_control_plane` to isolate the scheduler from the Android UI build.
* **IMPLEMENTED**: `docker-compose.yml` defining the local bridge environment linking the portable control plane container with a physical PostgreSQL 15 container.

## Deployment Inspection
* **OBSERVED**: The repository contained a `WEB_CONTROL_PLANE_ARCHITECTURE.md` and some Python parsing scripts. However, it did not contain an existing functional Firebase Functions structure, existing Node.js production web server, or pre-existing Docker compose setups.
* **INFERENCE**: Introducing a totally new Node.js/TypeScript architecture would create a destructive dual-stack maintenance burden since the Governor logic is already verified in Kotlin.
* **DECISION**: The JVM-based `:cloud_control_plane` module remains the canonical environment, deployed via Docker. It natively shares domain types while satisfying the requirement for cloud independence.

## Evidence Matrix
* **LOCAL_EXECUTION_VERIFIED**: The scheduler, idempotency rules, and kill switches run perfectly in JVM using SQLite.
* **CONTAINERIZED_VERIFIED**: The JVM artifact is isolated into a standalone Dockerfile.
* **POSTGRES_IMPLEMENTED_UNVERIFIED**: The `PostgresLedgerRepository` JDBC adapter is built and wired. It is pending physical verification against a live database instance.
* **REMOTE_DEPLOYMENT_UNVERIFIED**: The Docker image is pending deployment to an external cloud provider (e.g. Google Cloud Run).

## Next Operations
We have bridged the gap between a local test and a portable, Postgres-ready, containerized server API. 
The immediate next step is to execute Mission 17.2C.6 to rewire the Android application to point at this remote API (`/api/v1/mindstream`), permanently separating the Observer from the Engine.
