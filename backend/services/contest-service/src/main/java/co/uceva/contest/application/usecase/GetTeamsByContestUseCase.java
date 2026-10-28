package co.uceva.contest.application.usecase;

import java.util.List;
import java.util.UUID;

import co.uceva.contest.domain.model.Team;

/**
 * Puerto de entrada para el caso de uso: consultar los equipos inscritos en un concurso.
 */
public interface GetTeamsByContestUseCase {
    /**
     * Consultar los equipos inscritos en un concurso.
     *
     * @param contestId Identificador del concurso.
     * @return Lista de resultados.
     */
    List<Team> execute(UUID contestId);
}
