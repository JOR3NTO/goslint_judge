package co.uceva.contests_platform.contest.application.usecase.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contests_platform.contest.application.event.TeamScoreChangedEvent;
import co.uceva.contests_platform.contest.application.usecase.UpdateTeamScoreUseCase;
import co.uceva.contests_platform.contest.domain.exception.ContestNotFoundException;
import co.uceva.contests_platform.contest.domain.model.Contest;
import co.uceva.contests_platform.contest.domain.model.ScoreboardRow;
import co.uceva.contests_platform.contest.domain.model.Team;
import co.uceva.contests_platform.contest.domain.model.TeamStanding;
import co.uceva.contests_platform.contest.domain.model.TeamSubmission;
import co.uceva.contests_platform.contest.domain.repository.ContestProblemRepository;
import co.uceva.contests_platform.contest.domain.repository.ContestRepository;
import co.uceva.contests_platform.contest.domain.repository.TeamRepository;
import co.uceva.contests_platform.contest.infrastructure.client.SubmissionAdapter;

/**
 * Servicio de Aplicación que implementa el caso de uso: recalcular el puntaje y la penalización
 * de un equipo desde sus envíos.
 * <p>
 * Recalcular en lugar de sumar hace que repetir la operación (p. ej. un reintento de Rabbit)
 * no cuente dos veces. Solo si el resultado cambia se guarda y se avisa al scoreboard.
 * </p>
 */
@Service
public class UpdateTeamScoreUseCaseImpl implements UpdateTeamScoreUseCase {

    private final TeamRepository teamRepository;
    private final ContestRepository contestRepository;
    private final ContestProblemRepository contestProblemRepository;
    private final SubmissionAdapter submissionAdapter;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param teamRepository            Puerto de salida para consultar y guardar equipos.
     * @param contestRepository         Puerto de salida para consultar el inicio del concurso.
     * @param contestProblemRepository  Puerto de salida para consultar puntos y penalización de los problemas.
     * @param submissionAdapter         Adaptador para consultar los envíos del equipo.
     * @param applicationEventPublisher Publicador de eventos internos de la aplicación.
     */
    public UpdateTeamScoreUseCaseImpl(TeamRepository teamRepository, ContestRepository contestRepository,
            ContestProblemRepository contestProblemRepository, SubmissionAdapter submissionAdapter,
            ApplicationEventPublisher applicationEventPublisher) {
        this.teamRepository = teamRepository;
        this.contestRepository = contestRepository;
        this.contestProblemRepository = contestProblemRepository;
        this.submissionAdapter = submissionAdapter;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * Recalcular el puntaje y la penalización de un equipo desde sus envíos.
     *
     * @param teamId Identificador del equipo.
     */
    @Override
    @Transactional
    public void execute(UUID teamId) {
        // Un equipo borrado mientras se calificaba no tiene puntaje que actualizar; el veredicto se guarda igual.
        Team team = teamRepository.findById(teamId).orElse(null);
        if (team == null) {
            return;
        }
        Contest contest = contestRepository.findById(team.getContestId())
                .orElseThrow(() -> new ContestNotFoundException(team.getContestId()));
        List<TeamSubmission> submissions = submissionAdapter.findByTeamId(teamId);
        // Con la tabla congelada solo cuentan los envíos anteriores al congelamiento, aunque se califiquen
        // después: los posteriores no la mueven hasta liberarla, y el resultado es el mismo que tras un reinicio.
        if (contest.isScoreboardFrozen()) {
            submissions = submissions.stream().filter(s -> s.submittedAt().isBefore(contest.getFreezeTime())).toList();
        }

        TeamStanding standing = TeamStanding.calculate(submissions,
                contestProblemRepository.findAllByContestIdOrderByOrderIndexAsc(contest.getId()),
                contest.getStartTime());
        if (standing.teamScore().equals(team.getTeamScore())) {
            return;
        }

        team.updateScore(standing.teamScore());
        Team saved = teamRepository.save(team);
        applicationEventPublisher.publishEvent(new TeamScoreChangedEvent(new ScoreboardRow(saved, standing.lastSolvedAt())));
    }
}
