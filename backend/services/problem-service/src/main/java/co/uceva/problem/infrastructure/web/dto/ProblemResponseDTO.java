package co.uceva.problem.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de respuesta que expone los datos de un problema al cliente.
 *
 * @param id            Identificador del problema.
 * @param createdBy     Identificador del usuario creador.
 * @param title         Título del problema.
 * @param statement     Enunciado del problema.
 * @param timeLimitMs   Límite de tiempo en milisegundos.
 * @param memoryLimitKb Límite de memoria en kilobytes.
 * @param difficult     Nivel de dificultad.
 * @param createdAt     Fecha de creación.
 * @param inputFormat   Formato de entrada esperado.
 * @param outputFormat  Formato de salida esperado.
 */
public record ProblemResponseDTO(
        UUID id,
        UUID createdBy,
        String title,
        String statement,
        int timeLimitMs,
        int memoryLimitKb,
        int difficult,
        Instant createdAt,
        String inputFormat,
        String outputFormat
) {}