package co.uceva.contests_platform.contest.application.usecase;

import java.util.List;

import co.uceva.contests_platform.contest.domain.model.Team;

/**
 * Puerto de entrada para el caso de uso: consultar todos los equipos registrados.
 */
public interface GetAllTeamsUseCase {
    /**
     * Consultar todos los equipos registrados.
     *
     * @return Lista de resultados.
     */
    List<Team> execute();
}
