-- =============================================================================
-- V1 — Tablas base para problem-service: problems y test_cases
-- =============================================================================

CREATE TABLE IF NOT EXISTS problems (
    id               UUID                     PRIMARY KEY DEFAULT gen_random_uuid(),
    created_by       UUID                     NOT NULL,
    title            VARCHAR(255)             NOT NULL,
    statement        TEXT                     NOT NULL,
    time_limit_ms    INTEGER                  NOT NULL,
    memory_limit_kb  INTEGER                  NOT NULL,
    difficulty       INTEGER                  NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    input_format     TEXT                     NOT NULL,
    output_format    TEXT                     NOT NULL
);

CREATE TABLE IF NOT EXISTS test_cases (
    id               UUID                     PRIMARY KEY DEFAULT gen_random_uuid(),
    problem_id       UUID                     NOT NULL,
    expected_output  TEXT                     NOT NULL,
    order_index      INTEGER                  NOT NULL,
    is_sample        BOOLEAN                  NOT NULL DEFAULT FALSE,
    input            TEXT                     NOT NULL,
    output           TEXT                     NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_test_cases_problem FOREIGN KEY (problem_id)
        REFERENCES problems (id) ON DELETE CASCADE
);

-- Índices para optimizar las consultas del servicio
CREATE INDEX IF NOT EXISTS idx_problems_created_by
    ON problems (created_by);

CREATE INDEX IF NOT EXISTS idx_test_cases_problem_id_order_index
    ON test_cases (problem_id, order_index);

CREATE INDEX IF NOT EXISTS idx_test_cases_problem_sample
    ON test_cases (problem_id, is_sample, order_index);
