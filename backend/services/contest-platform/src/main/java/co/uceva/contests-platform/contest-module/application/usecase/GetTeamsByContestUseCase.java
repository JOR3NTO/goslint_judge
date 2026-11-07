package co.uceva.contestsplatform.contestmodule.application.usecase;

import java.util.List;
import java.util.UUID;

import co.uceva.contestsplatform.contestmodule.domain.model.Team;

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
