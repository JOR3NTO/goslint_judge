package co.uceva.contests_platform.contest.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.uceva.contests_platform.contest.application.event.ScoreboardFrozenEvent;
import co.uceva.contests_platform.contest.application.event.ScoreboardUnfrozenEvent;
import co.uceva.contests_platform.contest.application.event.TeamScoreChangedEvent;
import co.uceva.contests_platform.contest.application.service.ScoreboardCalculator;
import co.uceva.contests_platform.contest.application.usecase.UpdateTeamScoreUseCase;
import co.uceva.contests_platform.contest.application.usecase.impl.FreezeScoreboardUseCaseImpl;
import co.uceva.contests_platform.contest.application.usecase.impl.GetScoreboardUseCaseImpl;
import co.uceva.contests_platform.contest.application.usecase.impl.UnfreezeScoreboardUseCaseImpl;
import co.uceva.contests_platform.contest.application.usecase.impl.UpdateTeamScoreUseCaseImpl;
import co.uceva.contests_platform.contest.domain.model.Contest;
import co.uceva.contests_platform.contest.domain.model.ContestProblem;
import co.uceva.contests_platform.contest.domain.model.ContestStatus;
import co.uceva.contests_platform.contest.domain.model.ScoreboardRow;
import co.uceva.contests_platform.contest.domain.model.ScoreboardStatus;
import co.uceva.contests_platform.contest.domain.model.Team;
import co.uceva.contests_platform.contest.domain.model.TeamSubmission;
import co.uceva.contests_platform.contest.domain.repository.ContestProblemRepository;
import co.uceva.contests_platform.contest.domain.repository.ContestRepository;
import co.uceva.contests_platform.contest.domain.repository.TeamRepository;
import co.uceva.contests_platform.contest.domain.valueobject.BalloonColor;
import co.uceva.contests_platform.contest.domain.valueobject.ContestPeriod;
import co.uceva.contests_platform.contest.domain.valueobject.TeamName;
import co.uceva.contests_platform.contest.domain.valueobject.TeamScore;
import co.uceva.contests_platform.contest.infrastructure.client.SubmissionAdapter;
import co.uceva.contests_platform.contest.infrastructure.scoreboard.ScoreboardCache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

@DisplayName("Scoreboard congelado: tarea programada, solo envíos anteriores y liberación manual")
class ScoreboardFreezeTest {

    // Concurso de 2 h que empezó hace 90 min y se congela en los últimos 60: congelado desde hace 30 min.
    private static final Instant START = Instant.now().minus(90, ChronoUnit.MINUTES);
    private static final Instant FREEZE = START.plus(60, ChronoUnit.MINUTES);

    private final TeamRepository teamRepository = mock(TeamRepository.class);
    private final ContestRepository contestRepository = mock(ContestRepository.class);
    private final ContestProblemRepository contestProblemRepository = mock(ContestProblemRepository.class);
    private final SubmissionAdapter submissionAdapter = mock(SubmissionAdapter.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final ScoreboardCache cache = new ScoreboardCache();
    private final ScoreboardCalculator calculator =
            new ScoreboardCalculator(teamRepository, contestProblemRepository, submissionAdapter);

    private final Contest contest = contest(ScoreboardStatus.FROZEN);
    private final ContestProblem problemA = problem("A");
    private final ContestProblem problemB = problem("B");
    private final Team team = Team.builder().contestId(contest.getId()).teamName(new TeamName("Equipo"))
            .teamScore(TeamScore.of(2, 110)).build();

    {
        when(contestRepository.findById(contest.getId())).thenReturn(Optional.of(contest));
        when(contestProblemRepository.findAllByContestIdOrderByOrderIndexAsc(contest.getId()))
                .thenReturn(List.of(problemA, problemB));
        when(teamRepository.findAllByContestId(contest.getId())).thenReturn(List.of(team));
        when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
        // A resuelto en el minuto 10, antes del congelamiento; B en el minuto 70, ya congelado.
        when(submissionAdapter.findByTeamId(team.getId())).thenReturn(List.of(accepted(problemA, 10), accepted(problemB, 70)));
    }

    private Contest contest(ScoreboardStatus status) {
        return Contest.builder().id(UUID.randomUUID()).title("Maratón")
                .period(ContestPeriod.restore(START, START.plus(2, ChronoUnit.HOURS)))
                .status(ContestStatus.ACTIVE).freezeMinutes(60).scoreboardStatus(status).build();
    }

    private ContestProblem problem(String alias) {
        return ContestProblem.builder().contestId(UUID.randomUUID()).problemId(UUID.randomUUID()).alias(alias)
                .orderIndex(0).points(1).penalty(20).balloonColor(BalloonColor.of("#FF0000")).build();
    }

    private static TeamSubmission accepted(ContestProblem p, long minute) {
        return new TeamSubmission(p.getId(), true, false, START.plus(minute, ChronoUnit.MINUTES));
    }

    @Test
    @DisplayName("Tras un reinicio, la tabla congelada se calcula solo con los envíos anteriores al congelamiento")
    void frozenBoardIsCalculatedUntilFreezeTime() {
        var getScoreboard = new GetScoreboardUseCaseImpl(contestRepository, calculator, cache);

        Team shown = getScoreboard.execute(contest.getId()).get(0);

        assertThat(shown.getTeamScore()).isEqualTo(TeamScore.of(1, 10));
    }

    private void updateTeamScore() {
        new UpdateTeamScoreUseCaseImpl(teamRepository, contestRepository, contestProblemRepository,
                submissionAdapter, publisher).execute(team.getId());
    }

    @Test
    @DisplayName("Con la tabla congelada, un AC enviado después del congelamiento no cambia nada")
    void submissionsAfterFreezeDoNotChangeAnything() {
        team.updateScore(TeamScore.of(1, 10)); // ya contaba A; B se envió congelado

        updateTeamScore();

        verify(teamRepository, never()).save(any());
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Un envío anterior al congelamiento que se califica después sí cuenta, como tras un reinicio")
    void submissionsBeforeFreezeJudgedLateStillCount() {
        team.updateScore(TeamScore.ZERO); // A aún estaba pendiente al congelar
        when(teamRepository.save(team)).thenReturn(team);

        updateTeamScore();

        assertThat(team.getTeamScore()).isEqualTo(TeamScore.of(1, 10));
        verify(publisher).publishEvent(any(TeamScoreChangedEvent.class));
    }

    @Test
    @DisplayName("La tarea congela una tabla en vivo cuando llega la hora, y no antes")
    void freezesOnlyWhenDue() {
        Contest live = contest(ScoreboardStatus.LIVE);
        when(contestRepository.findById(live.getId())).thenReturn(Optional.of(live));
        when(contestRepository.save(live)).thenReturn(live);

        new FreezeScoreboardUseCaseImpl(contestRepository, publisher).execute(live.getId());

        assertThat(live.getScoreboardStatus()).isEqualTo(ScoreboardStatus.FROZEN);
        verify(publisher).publishEvent(new ScoreboardFrozenEvent(live));

        Contest notYet = Contest.builder().id(UUID.randomUUID()).title("Futuro")
                .period(ContestPeriod.restore(Instant.now(), Instant.now().plus(2, ChronoUnit.HOURS)))
                .status(ContestStatus.ACTIVE).freezeMinutes(60).build();
        when(contestRepository.findById(notYet.getId())).thenReturn(Optional.of(notYet));

        new FreezeScoreboardUseCaseImpl(contestRepository, publisher).execute(notYet.getId());

        assertThat(notYet.getScoreboardStatus()).isEqualTo(ScoreboardStatus.LIVE);
    }

    @Test
    @DisplayName("Al liberar se recalculan todos los equipos con todos sus envíos")
    void unfreezeRecalculatesEveryTeam() {
        UpdateTeamScoreUseCase updateTeamScore = mock(UpdateTeamScoreUseCase.class);
        when(contestRepository.save(contest)).thenReturn(contest);

        new UnfreezeScoreboardUseCaseImpl(contestRepository, teamRepository, updateTeamScore, publisher)
                .execute(contest.getId());

        assertThat(contest.getScoreboardStatus()).isEqualTo(ScoreboardStatus.UNFROZEN);
        verify(updateTeamScore).execute(team.getId());
        verify(publisher).publishEvent(new ScoreboardUnfrozenEvent(contest));
    }

    @Test
    @DisplayName("Se sirve la tabla congelada hasta liberarla; entonces se sirve la real")
    void frozenBoardIsServedUntilUnfrozen() {
        var getScoreboard = new GetScoreboardUseCaseImpl(contestRepository, calculator, cache);
        Team other = Team.builder().contestId(contest.getId()).teamName(new TeamName("Otro"))
                .teamScore(TeamScore.of(5, 0)).build();

        cache.freeze(contest.getId(), List.of(new ScoreboardRow(team, null)));
        assertThat(getScoreboard.execute(contest.getId())).containsExactly(team);

        cache.unfreeze(contest.getId(), List.of(new ScoreboardRow(other, Instant.now()), new ScoreboardRow(team, null)));
        assertThat(getScoreboard.execute(contest.getId())).containsExactly(other, team);
    }

    @Test
    @DisplayName("Los minutos de congelamiento no pueden superar la duración del concurso")
    void freezeMinutesWithinDuration() {
        Contest live = contest(ScoreboardStatus.LIVE);

        assertThatThrownBy(() -> live.changeFreezeMinutes(121)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> live.changeFreezeMinutes(-1)).isInstanceOf(IllegalArgumentException.class);
        live.changeFreezeMinutes(0);
        assertThat(live.getFreezeTime()).isNull();
    }
}
