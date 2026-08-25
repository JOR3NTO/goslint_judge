package co.uceva.submission.infrastructure.web.dto;

import co.uceva.shared.domain.ProgrammingLanguage;
import co.uceva.shared.domain.VerdictStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de respuesta que expone los datos de un envío al cliente.
 *
 * @param id              Identificador del envío.
 * @param teamId          Identificador del equipo que realizó el envío.
 * @param problemId       Identificador del problema asociado.
 * @param language        Lenguaje de programación del código fuente.
 * @param sourceCode      Código fuente enviado.
 * @param verdict         Veredicto actual de la evaluación.
 * @param executionTimeMs Tiempo de ejecución en milisegundos.
 * @param memoryUsedKb    Memoria utilizada en kilobytes.
 * @param codeSizeBytes   Tamaño del código fuente en bytes.
 * @param submittedAt     Fecha de recepción del envío.
 */
public record SubmissionResponseDTO(
        UUID id,
        UUID teamId,
        UUID problemId,
        ProgrammingLanguage language,
        String sourceCode,
        VerdictStatus verdict,
        int executionTimeMs,
        int memoryUsedKb,
        long codeSizeBytes,
        Instant submittedAt
) {}
