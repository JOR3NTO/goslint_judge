package co.uceva.contests_platform.contest.application.event;

import co.uceva.contests_platform.contest.domain.model.ScoreboardRow;

/**
 * Evento interno: el puntaje o la penalización de un equipo cambió y ya se guardó.
 * Se escucha después del commit para actualizar el scoreboard en caché.
 *
 * @param row Nueva fila del equipo en la tabla de posiciones.
 */
public record TeamScoreChangedEvent(ScoreboardRow row) {
}
