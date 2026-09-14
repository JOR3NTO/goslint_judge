package co.uceva.shared.domain.event;

import co.uceva.shared.domain.ProgrammingLanguage;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento de dominio compartido que representa la recepción de un nuevo envío
 * de código fuente por parte de {@code submission-service}.
 * <p>
 * Este evento es publicado una vez que el envío ha sido validado y persistido,
 * y puede ser consumido por otros servicios (principalmente {@code judge-service})
 * para iniciar el proceso de evaluación.
 * </p>
 *
 * @param submissionId Identificador único del envío recibido.
 * @param teamId       Identificador del equipo que realizó el envío.
 * @param problemId    Identificador del problema al que responde el envío.
 * @param language     Lenguaje de programación del código fuente.
 * @param sourceCode   Código fuente enviado por el estudiante.
 * @param submittedAt  Fecha y hora en la que se recibió el envío.
 */
public record SubmissionReceivedEvent(
        UUID submissionId,
        UUID teamId,
        UUID problemId,
        ProgrammingLanguage language,
        String sourceCode,
        Instant submittedAt
) {}
