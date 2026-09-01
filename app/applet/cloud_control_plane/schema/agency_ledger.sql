-- M. Engine - Phase 1 Persistent Remote State Schema
-- Mission 17.2B: Persistent Remote Autonomous Runtime

CREATE TABLE control_plane_state (
    id SERIAL PRIMARY KEY,
    autonomy_enabled BOOLEAN NOT NULL DEFAULT true,
    emergency_stop BOOLEAN NOT NULL DEFAULT false,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO control_plane_state (autonomy_enabled, emergency_stop) VALUES (true, false);

CREATE TABLE agency_runs (
    id UUID PRIMARY KEY,
    status VARCHAR(50) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    exit_reason TEXT
);

CREATE TABLE mindstream_entries (
    id UUID PRIMARY KEY,
    run_id UUID REFERENCES agency_runs(id),
    entry_type VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE opportunities (
    id UUID PRIMARY KEY,
    description TEXT NOT NULL,
    source VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE development_signals (
    id UUID PRIMARY KEY,
    signal_type VARCHAR(50) NOT NULL, -- e.g., NEW_REQUIREMENT, CORRECTION
    project VARCHAR(100) NOT NULL,
    intent TEXT NOT NULL,
    confidence FLOAT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'REQUIRES_RESEARCH',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE evidence_ledger (
    id UUID PRIMARY KEY,
    evidence_type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    authorization_level VARCHAR(50) NOT NULL,
    outcome VARCHAR(50) NOT NULL, -- CONFIRMED, CONTESTED, etc.
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE project_ecology (
    id UUID PRIMARY KEY,
    component_name VARCHAR(100) NOT NULL,
    state_snapshot TEXT NOT NULL,
    last_inspected TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE worker_results (
    id UUID PRIMARY KEY,
    worker_type VARCHAR(50) NOT NULL,
    task_id UUID NOT NULL,
    result_status VARCHAR(50) NOT NULL,
    artifacts_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
