package co.uceva.contest.application.usecase.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contest.application.usecase.GetAllTeamsUseCase;
import co.uceva.contest.domain.model.Team;
import co.uceva.contest.domain.repository.TeamRepository;

/**
 * Servicio de Aplicación que implementa el caso de uso: consultar todos los equipos registrados.
 */
@Service
public class GetAllTeamsUseCaseImpl implements GetAllTeamsUseCase {

    private final TeamRepository teamRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param teamRepository Puerto de salida para consultar equipos.
     */
    public GetAllTeamsUseCaseImpl(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    /**
     * Consultar todos los equipos registrados.
     *
     * @return Lista de resultados.
     */
    @Override
    @Transactional(readOnly = true)
    public List<Team> execute() {
        return teamRepository.findAll();
    }
}
