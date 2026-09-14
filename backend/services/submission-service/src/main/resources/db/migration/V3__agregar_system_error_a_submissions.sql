-- =============================================================================
-- V3 — Estado terminal de fallo del sistema (SYSTEM_ERROR)
--
-- Acompana a la HU-28 (actualizacion del veredicto). Hasta ahora el ciclo de vida
-- del envio solo contemplaba desenlaces felices: o el juez emitia un veredicto
-- (JUDGED) o el envio seguia esperando. Faltaba el tercer desenlace real, que es
-- que el motor de evaluacion agote sus reintentos sin conseguir evaluarlo.
--
-- Sin este estado, un envio averiado se queda para siempre en QUEUED y el
-- estudiante espera indefinidamente un veredicto que nadie va a emitir. Con el,
-- la espera se cierra y el cambio se le puede notificar como cualquier otro.
--
-- Un envio en SYSTEM_ERROR conserva su verdict en PENDING: el juez nunca llego a
-- pronunciarse sobre el codigo, y atribuirle un veredicto real culparia al
-- estudiante de un fallo de la plataforma.
-- =============================================================================

-- La restriccion de V2 enumera los estados validos, asi que hay que reemplazarla:
-- un UPDATE a SYSTEM_ERROR contra la version anterior seria rechazado por la base
-- de datos. Se elimina y se vuelve a crear en lugar de alterarla porque PostgreSQL
-- no permite modificar el predicado de un CHECK existente.
ALTER TABLE submissions
    DROP CONSTRAINT IF EXISTS ck_submissions_status;

ALTER TABLE submissions
    ADD CONSTRAINT ck_submissions_status
    CHECK (status IN ('PENDING', 'QUEUED', 'JUDGING', 'JUDGED', 'SYSTEM_ERROR'));
