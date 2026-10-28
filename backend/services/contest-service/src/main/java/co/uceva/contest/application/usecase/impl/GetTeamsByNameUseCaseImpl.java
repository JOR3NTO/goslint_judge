package co.uceva.contest.application.usecase.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contest.application.usecase.GetTeamsByNameUseCase;
import co.uceva.contest.domain.model.Team;
import co.uceva.contest.domain.repository.TeamRepository;

/**
 * Servicio de Aplicación que implementa la búsqueda de equipos por nombre dentro de un concurso.
 */
@Service
public class GetTeamsByNameUseCaseImpl implements GetTeamsByNameUseCase {

    private final TeamRepository teamRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param teamRepository Puerto de salida para consultar equipos.
     */
    public GetTeamsByNameUseCaseImpl(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    /**
     * Recupera los equipos del concurso cuyo nombre coincida con el criterio dado.
     *
     * @param contestId Identificador del concurso.
     * @param name      Nombre o fragmento a buscar.
     * @return Lista de equipos que coinciden.
     */
    @Override
    @Transactional(readOnly = true)
    public List<Team> execute(UUID contestId, String name) {
        return teamRepository.findAllByContestIdAndNameTeamContaining(contestId, name);
    }
}
