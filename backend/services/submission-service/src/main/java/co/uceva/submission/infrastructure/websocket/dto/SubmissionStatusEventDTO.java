package co.uceva.submission.infrastructure.websocket.dto;

import co.uceva.shared.domain.SubmissionStatus;
import co.uceva.shared.domain.VerdictStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO que viaja por el WebSocket con el estado actual de un envío.
 * <p>
 * Es el contrato del canal de notificación con el frontend. Lleva lo justo para
 * que la pantalla se actualice sin recargar: qué envío es, en qué estado quedó y
 * con qué métricas. Deliberadamente <strong>no</strong> incluye el código fuente,
 * que ya está en poder de quien lo envió y no tiene por qué recorrer la red otra
 * vez.
 * </p>
 * <p>
 * El campo {@code type} existe para que el cliente pueda distinguir esta
 * notificación de las que se añadan en el futuro sobre el mismo canal, sin tener
 * que deducirlo de la forma del mensaje.
 * </p>
 *
 * @param type            Discriminador del mensaje; siempre {@code SUBMISSION_STATUS_UPDATED}.
 * @param submissionId    Identificador del envío actualizado.
 * @param problemId       Identificador del problema al que responde el envío.
 * @param teamId          Identificador del equipo dueño del envío.
 * @param status          Estado del envío en el flujo de evaluación.
 * @param verdict         Veredicto actual de la evaluación.
 * @param executionTimeMs Tiempo de ejecución en milisegundos.
 * @param memoryUsedKb    Memoria utilizada en kilobytes.
 * @param occurredAt      Instante en que se registró el cambio de estado.
 */
public record SubmissionStatusEventDTO(
        String type,
        UUID submissionId,
        UUID problemId,
        UUID teamId,
        SubmissionStatus status,
        VerdictStatus verdict,
        int executionTimeMs,
        int memoryUsedKb,
        Instant occurredAt
) {

    /** Único tipo de mensaje que emite hoy el canal de estado de envíos. */
    public static final String SUBMISSION_STATUS_UPDATED = "SUBMISSION_STATUS_UPDATED";
}
