-- =============================================================================
-- V1 — Tabla base para contest-service: contests
-- =============================================================================

CREATE TABLE IF NOT EXISTS contests (
    id               UUID                     PRIMARY KEY DEFAULT gen_random_uuid(),
    created_by       UUID                     NOT NULL,
    title            VARCHAR(255)             NOT NULL,
    description      TEXT                     NOT NULL,
    password_hash    VARCHAR(255)             NOT NULL,
    start_time       TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time         TIMESTAMP WITH TIME ZONE NOT NULL,
    status           VARCHAR(20)              NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_contests_period CHECK (end_time > start_time)
);

CREATE INDEX IF NOT EXISTS idx_contests_created_by
    ON contests (created_by);
