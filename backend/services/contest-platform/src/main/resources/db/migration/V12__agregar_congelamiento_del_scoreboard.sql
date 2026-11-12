-- Congelamiento de la tabla de posiciones pública: cada concurso define cuántos minutos antes del fin
-- se congela (0 = nunca) y guarda el estado de la tabla, que cambia una tarea programada (LIVE → FROZEN)
-- y el organizador al liberar los resultados (FROZEN → UNFROZEN).
ALTER TABLE contests
    ADD COLUMN freeze_minutes    INTEGER     NOT NULL DEFAULT 0,
    ADD COLUMN scoreboard_status VARCHAR(20) NOT NULL DEFAULT 'LIVE';
