package co.uceva.contest.application.usecase;

import java.util.List;
import java.util.UUID;

import co.uceva.contest.domain.model.Team;

/**
 * Puerto de entrada para el caso de uso: consultar los equipos a los que pertenece un usuario.
 */
public interface GetTeamsByUserUseCase {
    /**
     * Consultar los equipos a los que pertenece un usuario.
     *
     * @param userId Identificador del usuario.
     * @return Lista de resultados.
     */
    List<Team> execute(UUID userId);
}
