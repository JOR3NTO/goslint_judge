package co.uceva.contests_platform.submission.infrastructure.client;

import co.uceva.contests_platform.contest.application.usecase.GetContestByIdUseCase;
import co.uceva.contests_platform.contest.application.usecase.GetProblemsByContestUseCase;
import co.uceva.contests_platform.contest.application.usecase.GetTeamByIdUseCase;
import co.uceva.contests_platform.contest.domain.exception.ContestNotFoundException;
import co.uceva.contests_platform.contest.domain.exception.TeamNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class ContestServiceDataAdapterTest {

    private static final String BASE = "http://contest-service";

    private final UUID teamId = UUID.randomUUID();
    private final UUID contestId = UUID.randomUUID();
    private final UUID userA = UUID.randomUUID();
    private final UUID userB = UUID.randomUUID();
    private final UUID problemId = UUID.randomUUID();

    @Mock
    private GetTeamByIdUseCase getTeamByIdUseCase;
    @Mock
    private GetContestByIdUseCase getContestByIdUseCase;
    @Mock
    private GetProblemsByContestUseCase getProblemsByContestUseCase;

    @InjectMocks
    private ContestServiceDataAdapter adapter;

    @Test
    void shouldReturnEmptyWhenTheTeamOrContestIsNotFound() {
        when(getContestByIdUseCase.execute(contestId)).thenThrow(ContestNotFoundException.class);
        when(getTeamByIdUseCase.execute(teamId)).thenThrow(TeamNotFoundException.class);

        assertThat(adapter.findTeam(teamId)).isEmpty();
        assertThat(adapter.findContest(contestId)).isEmpty();
    }

    @Test
    void shouldReturnAnEmptyListWhenTheContestOfTheProblemsIsNotFound() {
        when(getProblemsByContestUseCase.execute(contestId)).thenThrow(ContestNotFoundException.class);

        assertThat(adapter.findContestProblemIds(contestId)).isEmpty();
    }
}
