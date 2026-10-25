package co.uceva.contest.infrastructure.web.dto;

import java.util.UUID;

/**
 * DTO de respuesta con los datos de un problema asignado a un concurso.
 *
 * @param id           Identificador de la asignación.
 * @param contestId    Identificador del concurso.
 * @param problemId    Identificador del problema en problem-service.
 * @param alias        Alias visible del problema.
 * @param orderIndex   Posición del problema en el concurso.
 * @param points       Puntos del problema.
 * @param balloonColor Color del globo en formato {@code #RRGGBB}.
 */
public record ContestProblemResponseDTO(
    UUID id,
    UUID contestId,
    UUID problemId,
    String alias,
    int orderIndex,
    int points,
    String balloonColor
) {}
