package co.uceva.contests_platform.contest.infrastructure.web.dto;

/**
 * Cambio de un equipo en la tabla de posiciones, enviado por SSE.
 *
 * @param position Nueva posición del equipo (empieza en 1).
 * @param team     Equipo con su puntaje y penalización actualizados.
 */
public record ScoreboardChangeDTO(int position, TeamResponseDTO team) {
}
