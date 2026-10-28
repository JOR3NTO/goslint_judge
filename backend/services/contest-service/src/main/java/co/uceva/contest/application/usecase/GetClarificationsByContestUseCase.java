package co.uceva.contest.application.usecase;

import java.util.List;
import java.util.UUID;

import co.uceva.contest.domain.model.Clarification;

/**
 * Puerto de entrada para el caso de uso: consultar todas las aclaraciones de un concurso.
 * Las aclaraciones son públicas, así que cualquier usuario puede consultarlas.
 */
public interface GetClarificationsByContestUseCase {
    /**
     * Consultar las aclaraciones de todos los problemas de un concurso, de la más reciente a la más antigua.
     *
     * @param contestId Identificador del concurso.
     * @return Lista de resultados.
     */
    List<Clarification> execute(UUID contestId);
}
