package co.uceva.contestsplatform.contestmodule.application.usecase.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contestsplatform.contestmodule.application.usecase.GetTeamByIdUseCase;
import co.uceva.contestsplatform.contestmodule.domain.exception.TeamNotFoundException;
import co.uceva.contestsplatform.contestmodule.domain.model.Team;
import co.uceva.contestsplatform.contestmodule.domain.repository.TeamRepository;

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
