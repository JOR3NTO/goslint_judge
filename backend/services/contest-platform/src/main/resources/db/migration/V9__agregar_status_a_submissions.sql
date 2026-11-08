-- =============================================================================
-- V2 — Ciclo de vida del envio (columna status)
--
-- Acompana a la HU-27 (encolamiento del envio hacia el juez). El envio deja de
-- describirse solo con su veredicto y pasa a tener un estado propio dentro del
-- flujo de evaluacion: PENDING (persistido, aun no entregado al motor),
-- QUEUED (entrega confirmada por el broker), JUDGING y JUDGED.
--
-- La columna es NOT NULL en SubmissionEntity, asi que se agrega en tres pasos
-- para no romper contra una tabla que ya tiene filas: primero nullable, luego
-- relleno del historico, y solo entonces la restriccion.
-- =============================================================================

-- 1. La columna entra nullable: las filas existentes todavia no tienen estado.
ALTER TABLE submissions
    ADD COLUMN IF NOT EXISTS status VARCHAR(20);

-- 2. Relleno del historico. Un envio anterior a esta migracion nunca paso por el
--    encolamiento, asi que su estado se deduce del veredicto: si el juez ya se
--    pronuncio, el envio esta cerrado (JUDGED); si sigue en PENDING, es trabajo
--    de entrega sin completar y queda en PENDING para que el barrido de
--    reintentos (PendingSubmissionRetryScheduler) lo recoja y lo encole.
UPDATE submissions
   SET status = CASE WHEN verdict = 'PENDING' THEN 'PENDING' ELSE 'JUDGED' END
 WHERE status IS NULL;

-- 3. Ya sin nulos, la columna pasa a ser obligatoria, como la declara la entidad.
ALTER TABLE submissions
    ALTER COLUMN status SET NOT NULL;

-- 4. El ciclo de vida es cerrado: la base de datos rechaza cualquier valor que no
--    pertenezca al enum SubmissionStatus. Se comprueba antes de crearla porque la
--    columna puede venir ya creada por ddl-auto en entornos de desarrollo.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_submissions_status'
    ) THEN
        ALTER TABLE submissions
            ADD CONSTRAINT ck_submissions_status
            CHECK (status IN ('PENDING', 'QUEUED', 'JUDGING', 'JUDGED'));
    END IF;
END $$;

-- Indice parcial para el barrido de reintentos, que cada pocos segundos busca los
-- envios PENDING mas antiguos. Solo indexa esas filas, que son una minoria
-- transitoria, de modo que el barrido no recorre toda la tabla de envios.
CREATE INDEX IF NOT EXISTS idx_submissions_pending_submitted_at
    ON submissions (submitted_at)
    WHERE status = 'PENDING';
