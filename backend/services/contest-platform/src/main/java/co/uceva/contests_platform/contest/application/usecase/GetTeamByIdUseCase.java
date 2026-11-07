package co.uceva.contests_platform.contest.application.usecase;

import java.util.UUID;

import co.uceva.contests_platform.contest.domain.model.Team;

/**
 * Puerto de entrada para el caso de uso: consultar un equipo por su identificador.
 */
public interface GetTeamByIdUseCase {
    /**
     * Consultar un equipo por su identificador.
     *
     * @param teamId Identificador del equipo.
     * @return El equipo encontrado.
     */
    Team execute(UUID teamId);
}
