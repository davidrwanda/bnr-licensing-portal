-- V1: Initial schema for BNR Bank Licensing Portal
-- Author: David NTAMAKEMWA

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Role and status are stored as VARCHAR; Java enums handle validation at the application layer.

CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE applications (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    applicant_id         UUID         NOT NULL REFERENCES users(id),
    reviewer_id          UUID         REFERENCES users(id),
    approver_id          UUID         REFERENCES users(id),
    institution_name     VARCHAR(255) NOT NULL,
    institution_type     VARCHAR(100) NOT NULL,
    contact_address      TEXT,
    business_description TEXT,
    status               VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    version              INTEGER      NOT NULL DEFAULT 0,
    reviewer_notes       TEXT,
    decision_reason      TEXT,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    submitted_at         TIMESTAMPTZ,
    decided_at           TIMESTAMPTZ,

    -- A reviewer and approver must be different people (belt-and-suspenders)
    CONSTRAINT reviewer_ne_approver CHECK (reviewer_id IS NULL OR approver_id IS NULL OR reviewer_id <> approver_id)
);

CREATE TABLE documents (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id   UUID         NOT NULL REFERENCES applications(id),
    uploader_id      UUID         NOT NULL REFERENCES users(id),
    file_name        VARCHAR(255) NOT NULL,
    file_size        BIGINT       NOT NULL,
    mime_type        VARCHAR(100) NOT NULL,
    storage_path     TEXT         NOT NULL,
    document_version INTEGER      NOT NULL DEFAULT 1,
    superseded       BOOLEAN      NOT NULL DEFAULT FALSE,
    uploaded_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- BIGSERIAL so gap detection is possible; no UUID here to preserve ordering
CREATE SEQUENCE audit_logs_id_seq;

CREATE TABLE audit_logs (
    id               BIGINT      PRIMARY KEY DEFAULT nextval('audit_logs_id_seq'),
    application_id   UUID        REFERENCES applications(id),
    actor_id         UUID        NOT NULL REFERENCES users(id),
    actor_email      VARCHAR(255) NOT NULL,
    action           VARCHAR(100) NOT NULL,
    previous_status  VARCHAR(50),
    new_status       VARCHAR(50),
    metadata         TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id),
    token       TEXT        NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes for common queries
CREATE INDEX idx_applications_applicant   ON applications(applicant_id);
CREATE INDEX idx_applications_reviewer    ON applications(reviewer_id);
CREATE INDEX idx_applications_status      ON applications(status);
CREATE INDEX idx_documents_application    ON documents(application_id);
CREATE INDEX idx_audit_application        ON audit_logs(application_id);
CREATE INDEX idx_audit_actor              ON audit_logs(actor_id);
CREATE INDEX idx_audit_created            ON audit_logs(created_at DESC);
CREATE INDEX idx_refresh_tokens_user      ON refresh_tokens(user_id);
