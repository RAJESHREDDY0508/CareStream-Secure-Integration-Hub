# Database Schemas

All databases use **PostgreSQL 15**. PII columns are encrypted at the application layer (AES-256).

---

## `auth_db` — Auth Service

```sql
-- Users
CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username    VARCHAR(100) UNIQUE NOT NULL,
    email       VARCHAR(255) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,         -- BCrypt hashed
    role        VARCHAR(50) NOT NULL,          -- ADMIN | DOCTOR | SERVICE
    enabled     BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW()
);

-- Refresh tokens (persisted for revocation)
CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(512) UNIQUE NOT NULL,  -- SHA-256 of token
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked     BOOLEAN DEFAULT FALSE,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- Token blacklist (revoked JWTs before expiry)
CREATE TABLE token_blacklist (
    jti         VARCHAR(255) PRIMARY KEY,      -- JWT ID claim
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_blacklist_expires ON token_blacklist(expires_at);
```

---

## `patient_db` — Patient Service

```sql
-- Master patient record
CREATE TABLE patients (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id          VARCHAR(50) UNIQUE NOT NULL,  -- e.g. P-12345
    first_name          BYTEA NOT NULL,               -- AES-256 encrypted
    last_name           BYTEA NOT NULL,               -- AES-256 encrypted
    date_of_birth       BYTEA NOT NULL,               -- AES-256 encrypted
    insurance_id        BYTEA,                        -- AES-256 encrypted
    current_status      VARCHAR(50) NOT NULL,         -- ADMITTED | DISCHARGED | TRANSFERRED
    current_ward        VARCHAR(100),
    attending_physician VARCHAR(100),
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    updated_at          TIMESTAMPTZ DEFAULT NOW()
);

-- Patient event history (append-only)
CREATE TABLE patient_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id        VARCHAR(255) UNIQUE NOT NULL,  -- Kafka event ID (idempotency)
    patient_id      VARCHAR(50) NOT NULL,
    event_type      VARCHAR(50) NOT NULL,          -- ADMISSION | DISCHARGE | TRANSFER
    correlation_id  VARCHAR(255) NOT NULL,
    source          VARCHAR(100) NOT NULL,
    payload         JSONB NOT NULL,
    processed_at    TIMESTAMPTZ DEFAULT NOW(),
    kafka_partition INT,
    kafka_offset    BIGINT
);

CREATE INDEX idx_patients_patient_id ON patients(patient_id);
CREATE INDEX idx_patient_events_patient_id ON patient_events(patient_id);
CREATE INDEX idx_patient_events_type ON patient_events(event_type);
CREATE INDEX idx_patient_events_processed ON patient_events(processed_at DESC);
```

---

## `audit_db` — Audit Service

```sql
-- Immutable audit log (no UPDATE or DELETE permitted — enforced via pg policy)
CREATE TABLE audit_logs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    audit_id    VARCHAR(255) UNIQUE NOT NULL,
    event_id    VARCHAR(255),
    action      VARCHAR(100) NOT NULL,
    actor_id    VARCHAR(255) NOT NULL,
    actor_role  VARCHAR(50) NOT NULL,
    resource    VARCHAR(500),
    resource_id VARCHAR(255),
    outcome     VARCHAR(20) NOT NULL,           -- SUCCESS | FAILURE
    ip_address  INET,
    details     JSONB,
    timestamp   TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- Row-level security: no DELETE allowed
ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;
CREATE POLICY audit_no_delete ON audit_logs FOR DELETE USING (false);
CREATE POLICY audit_no_update ON audit_logs FOR UPDATE USING (false);

CREATE INDEX idx_audit_actor ON audit_logs(actor_id);
CREATE INDEX idx_audit_resource ON audit_logs(resource_id);
CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp DESC);
CREATE INDEX idx_audit_action ON audit_logs(action);
```

---

## `security_db` — Vulnerability, Threat Detection, Incident Services

```sql
-- Vulnerability findings
CREATE TABLE vulnerabilities (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    finding_id          VARCHAR(255) UNIQUE NOT NULL,
    scan_id             VARCHAR(255),
    cve_id              VARCHAR(50),
    severity            VARCHAR(20) NOT NULL,    -- CRITICAL | HIGH | MEDIUM | LOW
    affected_component  VARCHAR(255) NOT NULL,
    affected_service    VARCHAR(100) NOT NULL,
    description         TEXT,
    cvss_score          DECIMAL(4,2),
    sla_deadline        TIMESTAMPTZ NOT NULL,
    status              VARCHAR(50) DEFAULT 'OPEN', -- OPEN | IN_PROGRESS | REMEDIATED | ACCEPTED | OVERDUE
    assignee_id         UUID,
    ticket_id           VARCHAR(100),
    detected_at         TIMESTAMPTZ DEFAULT NOW(),
    remediated_at       TIMESTAMPTZ,
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    updated_at          TIMESTAMPTZ DEFAULT NOW()
);

-- Scan history
CREATE TABLE scans (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scan_id         VARCHAR(255) UNIQUE NOT NULL,
    scan_type       VARCHAR(50) NOT NULL,         -- DEPENDENCY | CONTAINER | SAST | DAST
    target_service  VARCHAR(100),
    status          VARCHAR(50) DEFAULT 'RUNNING',
    findings_count  INT DEFAULT 0,
    critical_count  INT DEFAULT 0,
    high_count      INT DEFAULT 0,
    started_at      TIMESTAMPTZ DEFAULT NOW(),
    completed_at    TIMESTAMPTZ
);

-- Incidents
CREATE TABLE incidents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id     VARCHAR(255) UNIQUE NOT NULL,
    alert_id        VARCHAR(255),
    title           VARCHAR(500) NOT NULL,
    severity        VARCHAR(20) NOT NULL,
    status          VARCHAR(50) DEFAULT 'OPEN',   -- OPEN | INVESTIGATING | CONTAINED | RESOLVED | CLOSED
    assigned_to     UUID,
    description     TEXT,
    timeline        JSONB DEFAULT '[]',           -- array of status change events
    opened_at       TIMESTAMPTZ DEFAULT NOW(),
    resolved_at     TIMESTAMPTZ,
    mttr_minutes    INT,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- Threat detection rules
CREATE TABLE detection_rules (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_id     VARCHAR(100) UNIQUE NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    rule_type   VARCHAR(50) NOT NULL,   -- THRESHOLD | PATTERN | ANOMALY
    conditions  JSONB NOT NULL,
    severity    VARCHAR(20) NOT NULL,
    enabled     BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_vulnerabilities_severity ON vulnerabilities(severity);
CREATE INDEX idx_vulnerabilities_status ON vulnerabilities(status);
CREATE INDEX idx_vulnerabilities_sla ON vulnerabilities(sla_deadline);
CREATE INDEX idx_incidents_status ON incidents(status);
CREATE INDEX idx_incidents_severity ON incidents(severity);
```
