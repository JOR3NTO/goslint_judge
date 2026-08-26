package co.uceva.shared.domain.event;

import co.uceva.shared.domain.VerdictStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento de dominio compartido que representa el resultado de la evaluación de
 * un envío por parte de {@code judge-service}.
 * <p>
 * Es la contrapartida de {@link SubmissionReceivedEvent}: cierra el ciclo que
 * aquel abre. {@code judge-service} lo publica al terminar de ejecutar el código
 * contra los casos de prueba, y {@code submission-service} lo consume para
 * registrar el veredicto junto a las métricas de ejecución.
 * </p>
 * <p>
 * El evento transporta el resultado ya calculado, nunca la orden de calcularlo:
 * quien lo recibe se limita a persistirlo y a notificarlo, sin volver a evaluar
 * nada.
 * </p>
 *
 * @param submissionId    Identificador único del envío evaluado.
 * @param verdict         Veredicto emitido por el motor de evaluación.
 * @param executionTimeMs Tiempo de ejecución medido en milisegundos.
 * @param memoryUsedKb    Memoria utilizada medida en kilobytes.
 * @param judgedAt        Fecha y hora en la que el juez terminó la evaluación.
 */
public record SubmissionJudgedEvent(
        UUID submissionId,
        VerdictStatus verdict,
        int executionTimeMs,
        int memoryUsedKb,
        Instant judgedAt
) {}
