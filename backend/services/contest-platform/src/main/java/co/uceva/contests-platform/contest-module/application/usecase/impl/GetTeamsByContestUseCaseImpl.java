package co.uceva.contestsplatform.contestmodule.application.usecase.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contestsplatform.contestmodule.application.usecase.GetTeamsByContestUseCase;
import co.uceva.contestsplatform.contestmodule.domain.model.Team;
import co.uceva.contestsplatform.contestmodule.domain.repository.TeamRepository;

/**
 * Servicio de Aplicación que implementa el caso de uso: consultar los equipos inscritos en un concurso.
 */
@Service
public class GetTeamsByContestUseCaseImpl implements GetTeamsByContestUseCase {

    private final TeamRepository teamRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param teamRepository Puerto de salida para consultar equipos.
     */
    public GetTeamsByContestUseCaseImpl(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    /**
     * Consultar los equipos inscritos en un concurso.
     *
     * @param contestId Identificador del concurso.
     * @return Lista de resultados.
     */
    @Override
    @Transactional(readOnly = true)
    public List<Team> execute(UUID contestId) {
        return teamRepository.findAllByContestId(contestId);
    }
}
