package co.uceva.problem.infrastructure.web.dto;

/**
 * DTO de solicitud para actualizar un problema existente.
 *
 * @param title         Nuevo título.
 * @param statement     Nuevo enunciado.
 * @param timeLimitMs   Nuevo límite de tiempo en milisegundos.
 * @param memoryLimitKb Nuevo límite de memoria en kilobytes.
 * @param difficult     Nueva dificultad.
 * @param inputFormat   Nuevo formato de entrada.
 * @param outputFormat  Nuevo formato de salida.
 */
public record UpdateProblemRequestDTO(
    String title,
    String statement,
    int timeLimitMs,
    int memoryLimitKb,
    int difficult,
    String inputFormat,
    String outputFormat
) {}