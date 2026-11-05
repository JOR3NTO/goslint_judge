-- =============================================================================
-- V1 — Tabla base de envios (submissions)
--
-- Linea base del esquema del submission-service. Hasta ahora la tabla se venia
-- creando de forma implicita (ddl-auto), sin ninguna definicion versionada en el
-- repositorio; esta migracion la fija tal y como existia antes de introducir el
-- ciclo de vida del envio (columna status, que llega en V2).
--
-- Se escribe de forma idempotente (IF NOT EXISTS) porque la base de datos es
-- compartida por todos los microservicios y la tabla puede existir ya en los
-- entornos que estuvieron corriendo con ddl-auto. En ese caso esta migracion no
-- toca nada y el trabajo real lo hace V2.
--
-- Los tipos coinciden exactamente con los que Hibernate espera para
-- SubmissionEntity bajo PostgreSQLDialect, porque el servicio arranca con
-- spring.jpa.hibernate.ddl-auto=validate: cualquier divergencia aborta el
-- arranque en lugar de corromper datos en silencio.
-- =============================================================================

CREATE TABLE IF NOT EXISTS submissions (
    id                UUID                        NOT NULL,
    team_id           UUID                        NOT NULL,
    problem_id        UUID                        NOT NULL,
    language          VARCHAR(255)                NOT NULL,
    source_code       TEXT                        NOT NULL,
    verdict           VARCHAR(255)                NOT NULL,
    execution_time_ms INTEGER                     NOT NULL,
    memory_used_kb    INTEGER                     NOT NULL,
    code_size_bytes   BIGINT                      NOT NULL,
    submitted_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_submissions PRIMARY KEY (id),
    CONSTRAINT ck_submissions_language CHECK (language IN ('C', 'CPP', 'JAVA', 'PYTHON')),
    CONSTRAINT ck_submissions_verdict CHECK (verdict IN (
        'PENDING', 'ACCEPTED', 'WRONG_ANSWER', 'TIME_LIMIT_EXCEEDED',
        'MEMORY_LIMIT_EXCEEDED', 'RUNTIME_ERROR', 'COMPILATION_ERROR'))
);

-- Historial de un equipo (GetSubmissionsByTeamUseCase).
CREATE INDEX IF NOT EXISTS idx_submissions_team_id
    ON submissions (team_id);

-- Envios de un problema y, por prefijo, los de un equipo dentro de ese problema
-- (GetSubmissionsByProblemUseCase, GetSubmissionHistoryUseCase y la deteccion de
-- duplicados de SubmitCodeUseCase).
CREATE INDEX IF NOT EXISTS idx_submissions_problem_id_team_id
    ON submissions (problem_id, team_id);
