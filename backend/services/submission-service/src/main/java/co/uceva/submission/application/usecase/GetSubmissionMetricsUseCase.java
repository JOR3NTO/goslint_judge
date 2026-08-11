package co.uceva.submission.application.usecase;

import co.uceva.shared.domain.VerdictStatus;
import co.uceva.submission.domain.model.Submission;

import java.util.UUID;

/**
 * Puerto de entrada para el caso de uso de consulta de métricas de un envío.
 */
public interface GetSubmissionMetricsUseCase {

    /**
     * Recupera las métricas de evaluación de un envío específico.
     *
     * @param submissionId Identificador del envío.
     * @return Las métricas del envío.
     */
    SubmissionMetrics execute(UUID submissionId);

    /**
     * Registro que agrupa las métricas de evaluación de un envío.
     *
     * @param submissionId   Identificador del envío.
     * @param verdict        Veredicto emitido por el juez.
     * @param executionTimeMs Tiempo de ejecución en milisegundos.
     * @param memoryUsedKb   Memoria utilizada en kilobytes.
     * @param codeSizeBytes  Tamaño del código fuente en bytes.
     */
    record SubmissionMetrics(
            UUID submissionId,
            VerdictStatus verdict,
            int executionTimeMs,
            int memoryUsedKb,
            long codeSizeBytes
    ) {}
}
