package co.uceva.contest.application.usecase.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contest.application.usecase.GetTeamsByUserUseCase;
import co.uceva.contest.domain.model.Team;
import co.uceva.contest.domain.repository.TeamRepository;

/**
 * Servicio de Aplicación que implementa el caso de uso: consultar los equipos a los que pertenece un usuario.
 */
@Service
public class GetTeamsByUserUseCaseImpl implements GetTeamsByUserUseCase {

    private final TeamRepository teamRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param teamRepository Puerto de salida para consultar equipos.
     */
    public GetTeamsByUserUseCaseImpl(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    /**
     * Consultar los equipos a los que pertenece un usuario.
     *
     * @param userId Identificador del usuario.
     * @return Lista de resultados.
     */
    @Override
    @Transactional(readOnly = true)
    public List<Team> execute(UUID userId) {
        return teamRepository.findAllByUserId(userId);
    }
}
