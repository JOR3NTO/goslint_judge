package co.uceva.contests_platform.contest.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import co.uceva.contests_platform.contest.application.usecase.impl.GetScoreboardUseCaseImpl;
import co.uceva.contests_platform.contest.domain.model.Team;
import co.uceva.contests_platform.contest.domain.repository.TeamRepository;
import co.uceva.contests_platform.contest.domain.valueobject.TeamName;
import co.uceva.contests_platform.contest.domain.valueobject.TeamScore;
import co.uceva.contests_platform.contest.infrastructure.client.SubmissionAdapter;
import co.uceva.contests_platform.submission.application.usecase.GetSubmissionsByTeamUseCase;
import co.uceva.contests_platform.submission.domain.model.Submission;
import co.uceva.shared.domain.VerdictStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Scoreboard: desempate por la hora del último problema resuelto")
class ScoreboardUseCaseTest {

    private static final UUID CONTEST = UUID.randomUUID();
    private static final Instant T0 = Instant.parse("2026-11-12T10:00:00Z");

    private final TeamRepository teamRepository = mock(TeamRepository.class);
    private final GetSubmissionsByTeamUseCase getSubmissions = mock(GetSubmissionsByTeamUseCase.class);
    private final GetScoreboardUseCaseImpl useCase =
            new GetScoreboardUseCaseImpl(teamRepository, new SubmissionAdapter(getSubmissions));

    private static Team team(String name) {
        return Team.builder().contestId(CONTEST).teamName(new TeamName(name)).teamScore(TeamScore.of(1, 20)).build();
    }

    private static Submission submission(UUID problemId, VerdictStatus verdict, long minutes) {
        Submission s = mock(Submission.class);
        when(s.getProblemId()).thenReturn(problemId);
        when(s.getVerdict()).thenReturn(verdict);
        when(s.getSubmittedAt()).thenReturn(T0.plusSeconds(minutes * 60));
        return s;
    }

    @Test
    @DisplayName("Con igual puntaje y penalización gana quien aceptó primero; los reenvíos aceptados no cuentan")
    void tieBrokenByFirstAcceptedOfLastProblem() {
        Team late = team("Tardios");
        Team early = team("Tempranos");
        Team none = team("SinAceptados");
        UUID problemA = UUID.randomUUID();
        UUID problemB = UUID.randomUUID();
        when(teamRepository.findAllByContestId(CONTEST))
                .thenReturn(List.of(none, late, early));
        // "late" aceptó A en el minuto 30 y lo reenvió aceptado en el 5: solo cuenta el primero (5).
        List<Submission> lateSubmissions = List.of(
                submission(problemA, VerdictStatus.ACCEPTED, 30),
                submission(problemA, VerdictStatus.ACCEPTED, 5));
        when(getSubmissions.execute(late.getId())).thenReturn(lateSubmissions);
        // "early" resolvió B (problema distinto) en el minuto 3; su WA anterior no cuenta.
        List<Submission> earlySubmissions = List.of(
                submission(problemB, VerdictStatus.WRONG_ANSWER, 1),
                submission(problemB, VerdictStatus.ACCEPTED, 3));
        when(getSubmissions.execute(early.getId())).thenReturn(earlySubmissions);
        when(getSubmissions.execute(none.getId())).thenReturn(List.of());

        assertThat(useCase.execute(CONTEST)).containsExactly(early, late, none);
    }
}
