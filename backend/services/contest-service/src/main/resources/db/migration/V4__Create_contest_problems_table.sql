-- =============================================================================
-- V4 — Problemas asignados a un concurso (con alias, posición, puntos y color de globo)
-- =============================================================================

CREATE TABLE IF NOT EXISTS contest_problems (
    id             UUID         PRIMARY KEY,
    contest_id     UUID         NOT NULL REFERENCES contests (id) ON DELETE CASCADE,
    -- Identificador en problem-service; sin FK porque vive en otro servicio.
    problem_id     UUID         NOT NULL,
    alias          VARCHAR(10)  NOT NULL,
    order_index    INTEGER      NOT NULL,
    points         INTEGER      NOT NULL DEFAULT 1,
    balloon_color  VARCHAR(7)   NOT NULL,

    -- Un problema y un alias solo una vez por concurso, también ante peticiones simultáneas.
    CONSTRAINT uq_contest_problems_problem UNIQUE (contest_id, problem_id),
    CONSTRAINT uq_contest_problems_alias UNIQUE (contest_id, alias),
    CONSTRAINT chk_contest_problems_values CHECK (order_index >= 0 AND points > 0),
    CONSTRAINT chk_contest_problems_color CHECK (balloon_color ~ '^#[0-9A-F]{6}$')
);

CREATE INDEX IF NOT EXISTS idx_contest_problems_order
    ON contest_problems (contest_id, order_index);
