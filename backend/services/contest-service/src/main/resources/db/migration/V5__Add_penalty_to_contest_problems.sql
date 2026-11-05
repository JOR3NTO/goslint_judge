-- =============================================================================
-- V5 — Penalización por intento fallido de cada problema del concurso
-- =============================================================================

-- Minutos que se suman por cada envío incorrecto previo a resolver el problema (ICPC: 20).
ALTER TABLE contest_problems
    ADD COLUMN IF NOT EXISTS penalty INTEGER NOT NULL DEFAULT 20;

ALTER TABLE contest_problems
    DROP CONSTRAINT IF EXISTS chk_contest_problems_penalty;
ALTER TABLE contest_problems
    ADD CONSTRAINT chk_contest_problems_penalty CHECK (penalty >= 0);
