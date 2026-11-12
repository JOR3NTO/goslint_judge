package co.uceva.contests_platform.contest.application.usecase.impl;

import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contests_platform.contest.application.event.ScoreboardUnfrozenEvent;
import co.uceva.contests_platform.contest.application.usecase.UnfreezeScoreboardUseCase;
import co.uceva.contests_platform.contest.application.usecase.UpdateTeamScoreUseCase;
import co.uceva.contests_platform.contest.domain.exception.ContestNotFoundException;
import co.uceva.contests_platform.contest.domain.model.Contest;
import co.uceva.contests_platform.contest.domain.model.ScoreboardStatus;
import co.uceva.contests_platform.contest.domain.repository.ContestRepository;
import co.uceva.contests_platform.contest.domain.repository.TeamRepository;

/**
 * Servicio de Aplicación que libera los resultados reales de un concurso con la tabla congelada.
 */
@Service
public class UnfreezeScoreboardUseCaseImpl implements UnfreezeScoreboardUseCase {

    private final ContestRepository contestRepository;
    private final TeamRepository teamRepository;
    private final UpdateTeamScoreUseCase updateTeamScoreUseCase;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param contestRepository         Puerto de salida para consultar y persistir concursos.
     * @param teamRepository            Puerto de salida para consultar los equipos del concurso.
     * @param updateTeamScoreUseCase    Recalcula el puntaje de cada equipo con todos sus envíos.
     * @param applicationEventPublisher Publicador de eventos internos de la aplicación.
     */
    public UnfreezeScoreboardUseCaseImpl(ContestRepository contestRepository, TeamRepository teamRepository,
            UpdateTeamScoreUseCase updateTeamScoreUseCase, ApplicationEventPublisher applicationEventPublisher) {
        this.contestRepository = contestRepository;
        this.teamRepository = teamRepository;
        this.updateTeamScoreUseCase = updateTeamScoreUseCase;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * {@inheritDoc}
     *
     * @throws ContestNotFoundException si el concurso no existe.
     */
    @Override
    @Transactional
    public Contest execute(UUID contestId) {
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new ContestNotFoundException(contestId));
        if (!contest.isScoreboardFrozen()) {
            return contest;
        }
        contest.setScoreboardStatus(ScoreboardStatus.UNFROZEN);
        Contest saved = contestRepository.save(contest);
        // Durante el congelamiento no se recalculó nada: se ponen al día todos los equipos antes de publicar.
        teamRepository.findAllByContestId(contestId).forEach(t -> updateTeamScoreUseCase.execute(t.getId()));
        applicationEventPublisher.publishEvent(new ScoreboardUnfrozenEvent(saved));
        return saved;
    }
}
