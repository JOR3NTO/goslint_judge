package co.uceva.contest.application.usecase;

import java.util.List;
import java.util.UUID;

import co.uceva.contest.domain.model.ContestProblem;

/**
 * Puerto de entrada para el caso de uso: consultar los problemas de un concurso.
 */
public interface GetProblemsByContestUseCase {
    /**
     * Consultar los problemas de un concurso en su orden de presentación.
     *
     * @param contestId Identificador del concurso.
     * @return Lista de resultados.
     */
    List<ContestProblem> execute(UUID contestId);
}
