package co.uceva.problem.infrastructure.web.dto;

import java.util.UUID;

/**
 * DTO de solicitud para crear un nuevo problema.
 *
 * @param createdBy     Identificador del usuario creador.
 * @param title         Título del problema.
 * @param statement     Enunciado del problema.
 * @param timeLimitMs   Límite de tiempo en milisegundos.
 * @param memoryLimitKb Límite de memoria en kilobytes.
 * @param difficult     Nivel de dificultad.
 * @param inputFormat   Formato de entrada esperado.
 * @param outputFormat  Formato de salida esperado.
 */
public record CreateProblemRequestDTO(
    UUID createdBy,
    String title,
    String statement,
    int timeLimitMs,
    int memoryLimitKb,
    int difficult,
    String inputFormat,
    String outputFormat
) {}
