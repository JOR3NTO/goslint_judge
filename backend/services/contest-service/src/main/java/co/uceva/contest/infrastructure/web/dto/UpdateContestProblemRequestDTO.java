package co.uceva.contest.infrastructure.web.dto;

/**
 * DTO de solicitud para actualizar un problema de un concurso.
 *
 * @param alias        Alias visible del problema.
 * @param orderIndex   Posición del problema en el concurso.
 * @param points       Puntos del problema.
 * @param penalty Minutos de penalización por intento fallido; si se omite, se conserva la actual.
 * @param balloonColor Color del globo en formato {@code #RRGGBB}.
 */
public record UpdateContestProblemRequestDTO(
    String alias,
    int orderIndex,
    int points,
    Integer penalty,
    String balloonColor
) {}
