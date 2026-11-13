package co.uceva.contests_platform.contest.application.event;

import co.uceva.contests_platform.contest.domain.model.Contest;

/**
 * Evento interno: el organizador liberó los resultados reales del concurso y ya se guardó.
 * Se escucha después del commit para publicar la tabla real por SSE.
 *
 * @param contest Concurso liberado.
 */
public record ScoreboardUnfrozenEvent(Contest contest) {
}
