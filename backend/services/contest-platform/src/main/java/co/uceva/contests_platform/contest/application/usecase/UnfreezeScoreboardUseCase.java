package co.uceva.contests_platform.contest.application.usecase;

import java.util.UUID;

import co.uceva.contests_platform.contest.domain.model.Contest;

/**
 * Puerto de entrada para que el organizador libere los resultados reales de un concurso congelado.
 */
public interface UnfreezeScoreboardUseCase {
    /**
     * Pasa la tabla de {@code FROZEN} a {@code UNFROZEN}; si no está congelada no hace nada.
     *
     * @param contestId Identificador del concurso.
     * @return El concurso, con su estado de tabla actual.
     */
    Contest execute(UUID contestId);
}
