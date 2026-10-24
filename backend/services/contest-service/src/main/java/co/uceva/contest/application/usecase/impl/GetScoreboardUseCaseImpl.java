package co.uceva.contest.application.usecase.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contest.application.usecase.GetScoreboardUseCase;
import co.uceva.contest.domain.model.Team;
import co.uceva.contest.domain.repository.TeamRepository;

/**
 * Servicio de Aplicación que implementa el caso de uso: consultar la tabla de posiciones de un concurso (mayor puntaje y menor penalización primero).
 */
@Service
public class GetScoreboardUseCaseImpl implements GetScoreboardUseCase {

    private final TeamRepository teamRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param teamRepository Puerto de salida para consultar equipos.
     */
    public GetScoreboardUseCaseImpl(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    /**
     * Consultar la tabla de posiciones de un concurso (mayor puntaje y menor penalización primero).
     *
     * @param contestId Identificador del concurso.
     * @return Lista de resultados.
     */
    @Override
    @Transactional(readOnly = true)
    public List<Team> execute(UUID contestId) {
        return teamRepository.findAllByContestIdOrderByScoreDescPenaltyTimeAsc(contestId);
    }
}
