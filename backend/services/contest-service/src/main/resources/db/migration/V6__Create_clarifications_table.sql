-- =============================================================================
-- V6 — Aclaraciones: preguntas de los equipos sobre los problemas del concurso
-- =============================================================================

CREATE TABLE IF NOT EXISTS clarifications (
    id           UUID                     PRIMARY KEY,
    problem_id   UUID                     NOT NULL REFERENCES contest_problems (id) ON DELETE CASCADE,
    team_id      UUID                     NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    question     TEXT                     NOT NULL,
    answer       TEXT,
    status       VARCHAR(20)              NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    answered_at  TIMESTAMP WITH TIME ZONE,

    CONSTRAINT chk_clarifications_status CHECK (status IN ('PENDING', 'ANSWERED')),
    -- Respondida si y solo si tiene respuesta y fecha de respuesta.
    CONSTRAINT chk_clarifications_answered CHECK (
        (status = 'ANSWERED' AND answer IS NOT NULL AND answered_at IS NOT NULL)
        OR (status = 'PENDING' AND answer IS NULL AND answered_at IS NULL)),
    CONSTRAINT chk_clarifications_length CHECK (
        char_length(question) <= 2000 AND (answer IS NULL OR char_length(answer) <= 2000))
);

CREATE INDEX IF NOT EXISTS idx_clarifications_problem
    ON clarifications (problem_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_clarifications_team
    ON clarifications (team_id);
