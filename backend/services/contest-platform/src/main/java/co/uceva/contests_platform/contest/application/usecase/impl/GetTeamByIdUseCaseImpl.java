package co.uceva.contests_platform.contest.application.usecase.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contests_platform.contest.application.usecase.GetTeamByIdUseCase;
import co.uceva.contests_platform.contest.domain.exception.TeamNotFoundException;
import co.uceva.contests_platform.contest.domain.model.Team;
import co.uceva.contests_platform.contest.domain.repository.TeamRepository;

/**
 * Servicio de Aplicación que implementa el caso de uso: consultar un equipo por su identificador.
 */
@Service
public class GetTeamByIdUseCaseImpl implements GetTeamByIdUseCase {

    private final TeamRepository teamRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param teamRepository Puerto de salida para consultar equipos.
     */
    public GetTeamByIdUseCaseImpl(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    /**
     * Consultar un equipo por su identificador.
     *
     * @param teamId Identificador del equipo.
     * @return El equipo encontrado.
     * @throws TeamNotFoundException Si el equipo no existe.
     */
    @Override
    @Transactional(readOnly = true)
    public Team execute(UUID teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamNotFoundException(teamId));
    }
}
