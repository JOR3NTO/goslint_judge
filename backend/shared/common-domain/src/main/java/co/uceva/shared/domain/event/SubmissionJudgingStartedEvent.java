package co.uceva.shared.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento de dominio compartido que señala que {@code judge-service} tomó un
 * envío y comenzó a evaluarlo.
 * <p>
 * Se ubica entre {@link SubmissionReceivedEvent} y {@link SubmissionJudgedEvent}
 * en el ciclo de vida del envío: no transporta ningún resultado, solo avisa de
 * que el procesamiento empezó, para que {@code submission-service} pueda
 * reflejarlo como {@code status = JUDGING} en lugar de dejar al envío
 * aparentando seguir «en cola» mientras el juez ya lo está ejecutando.
 * </p>
 *
 * @param submissionId Identificador único del envío que se empezó a evaluar.
 * @param startedAt    Fecha y hora en la que el juez tomó el envío.
 */
public record SubmissionJudgingStartedEvent(
        UUID submissionId,
        Instant startedAt
) {}
