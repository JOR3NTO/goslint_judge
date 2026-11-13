package co.uceva.contests_platform.contest.application;

import static org.assertj.core.api.Assertions.assertThat;
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

import co.uceva.contests_platform.contest.application.event.TeamScoreChangedEvent;
import co.uceva.contests_platform.contest.application.service.ScoreboardCalculator;
import co.uceva.contests_platform.contest.application.usecase.impl.GetScoreboardUseCaseImpl;
import co.uceva.contests_platform.contest.application.usecase.impl.UpdateTeamScoreUseCaseImpl;
import co.uceva.contests_platform.contest.domain.model.Contest;
import co.uceva.contests_platform.contest.domain.model.ContestProblem;
import co.uceva.contests_platform.contest.domain.model.ContestStatus;
import co.uceva.contests_platform.contest.domain.model.ScoreboardRow;
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

@DisplayName("Scoreboard: recálculo de un equipo, caché y desempate")
class ScoreboardUseCaseTest {

    private static final Instant START = Instant.parse("2026-11-12T10:00:00Z");

    private final Contest contest = Contest.builder().id(UUID.randomUUID()).title("Maratón")
            .period(ContestPeriod.restore(START, START.plus(5, ChronoUnit.HOURS)))
            .status(ContestStatus.ACTIVE).build();
    private final ContestProblem problemA = ContestProblem.builder().contestId(contest.getId())
            .problemId(UUID.randomUUID()).alias("A").orderIndex(0).points(1).penalty(20)
            .balloonColor(BalloonColor.of("#FF0000")).build();

    private final TeamRepository teamRepository = mock(TeamRepository.class);
    private final ContestRepository contestRepository = mock(ContestRepository.class);
    private final ContestProblemRepository contestProblemRepository = mock(ContestProblemRepository.class);
    private final SubmissionAdapter submissionAdapter = mock(SubmissionAdapter.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final ScoreboardCache cache = new ScoreboardCache();

    private final GetScoreboardUseCaseImpl getScoreboard = new GetScoreboardUseCaseImpl(contestRepository,
            new ScoreboardCalculator(teamRepository, contestProblemRepository, submissionAdapter), cache);
    private final UpdateTeamScoreUseCaseImpl updateTeamScore = new UpdateTeamScoreUseCaseImpl(
            teamRepository, contestRepository, contestProblemRepository, submissionAdapter, publisher);

    {
        when(contestRepository.findById(contest.getId())).thenReturn(Optional.of(contest));
        when(contestProblemRepository.findAllByContestIdOrderByOrderIndexAsc(contest.getId()))
                .thenReturn(List.of(problemA));
    }

    private Team team(String name, TeamScore score) {
        return Team.builder().contestId(contest.getId()).teamName(new TeamName(name)).teamScore(score).build();
    }

    private TeamSubmission acceptedA(long minute) {
        return new TeamSubmission(problemA.getId(), true, false, START.plusSeconds(minute * 60));
    }

    @Test
    @DisplayName("Con igual puntaje y penalización gana quien resolvió antes su último problema")
    void tieBrokenByLastSolvedAt() {
        Team late = team("Tardios", TeamScore.of(1, 20));
        Team early = team("Tempranos", TeamScore.of(1, 20));
        Team none = team("SinAceptados", TeamScore.ZERO);
        when(teamRepository.findAllByContestId(contest.getId())).thenReturn(List.of(none, late, early));
        when(submissionAdapter.findByTeamId(late.getId())).thenReturn(List.of(acceptedA(30)));
        when(submissionAdapter.findByTeamId(early.getId())).thenReturn(List.of(acceptedA(3)));
        when(submissionAdapter.findByTeamId(none.getId())).thenReturn(List.of());

        assertThat(getScoreboard.execute(contest.getId())).containsExactly(early, late, none);
    }

    @Test
    @DisplayName("Si el resultado no cambia no se guarda ni se avisa al scoreboard")
    void unchangedScoreDoesNothing() {
        Team team = team("Equipo", TeamScore.of(1, 10));
        when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
        when(submissionAdapter.findByTeamId(team.getId())).thenReturn(List.of(acceptedA(10)));

        updateTeamScore.execute(team.getId());

        verify(teamRepository, never()).save(any());
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Si el resultado cambia se guarda y se publica la nueva fila del equipo")
    void changedScoreIsSavedAndPublished() {
        Team team = team("Equipo", TeamScore.ZERO);
        when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
        when(teamRepository.save(team)).thenReturn(team);
        when(submissionAdapter.findByTeamId(team.getId())).thenReturn(List.of(acceptedA(10)));

        updateTeamScore.execute(team.getId());

        assertThat(team.getTeamScore()).isEqualTo(TeamScore.of(1, 10));
        verify(publisher).publishEvent(new TeamScoreChangedEvent(new ScoreboardRow(team, START.plusSeconds(600))));
    }

    @Test
    @DisplayName("La caché reposiciona solo al equipo que cambió")
    void cacheRepositionsChangedTeam() {
        Team first = team("Primero", TeamScore.of(1, 10));
        Team second = team("Segundo", TeamScore.ZERO);
        when(teamRepository.findAllByContestId(contest.getId())).thenReturn(List.of(first, second));
        when(submissionAdapter.findByTeamId(first.getId())).thenReturn(List.of(acceptedA(10)));
        when(submissionAdapter.findByTeamId(second.getId())).thenReturn(List.of());
        assertThat(getScoreboard.execute(contest.getId())).containsExactly(first, second);

        second.updateScore(TeamScore.of(1, 5));
        cache.update(new ScoreboardRow(second, START.plusSeconds(300)));

        assertThat(getScoreboard.execute(contest.getId())).containsExactly(second, first);
    }
}
