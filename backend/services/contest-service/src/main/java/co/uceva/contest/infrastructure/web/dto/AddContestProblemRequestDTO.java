package co.uceva.contest.infrastructure.web.dto;

import java.util.UUID;

/**
 * DTO de solicitud para asignar un problema a un concurso.
 *
 * @param problemId    Identificador del problema en problem-service.
 * @param alias        Alias visible del problema (A, B, C...).
 * @param orderIndex   Posición del problema en el concurso.
 * @param points       Puntos del problema; si se omite, 1.
 * @param penalty Minutos de penalización por intento fallido; si se omite, 20.
 * @param balloonColor Color del globo en formato {@code #RRGGBB}.
 */
public record AddContestProblemRequestDTO(
    UUID problemId,
    String alias,
    int orderIndex,
    Integer points,
    Integer penalty,
    String balloonColor
) {}
