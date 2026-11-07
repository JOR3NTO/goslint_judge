package co.uceva.contestsplatform.contestmodule.application.usecase;

import java.util.List;

import co.uceva.contestsplatform.contestmodule.domain.model.Team;

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
