package co.uceva.contests_platform.contest.application.event;

import co.uceva.contests_platform.contest.domain.model.Contest;

/**
 * Evento interno: la tabla pública del concurso quedó congelada y ya se guardó.
 * Se escucha después del commit para congelar el scoreboard en caché y avisar por SSE.
 *
 * @param contest Concurso congelado.
 */
public record ScoreboardFrozenEvent(Contest contest) {
}
