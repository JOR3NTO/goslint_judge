package co.uceva.submission.infrastructure.web.dto;

import co.uceva.shared.domain.VerdictStatus;

import java.util.UUID;

/**
 * DTO de respuesta que expone las métricas de evaluación de un envío.
 *
 * @param submissionId    Identificador del envío.
 * @param verdict         Veredicto emitido por el juez.
 * @param executionTimeMs Tiempo de ejecución en milisegundos.
 * @param memoryUsedKb    Memoria utilizada en kilobytes.
 * @param codeSizeBytes   Tamaño del código fuente en bytes.
 */
public record SubmissionMetricsResponseDTO(
        UUID submissionId,
        VerdictStatus verdict,
        int executionTimeMs,
        int memoryUsedKb,
        long codeSizeBytes
) {}
