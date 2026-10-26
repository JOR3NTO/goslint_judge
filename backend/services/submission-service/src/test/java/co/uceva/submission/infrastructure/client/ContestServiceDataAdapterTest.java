package co.uceva.submission.infrastructure.client;

import co.uceva.submission.application.exception.ContestServiceUnavailableException;
import co.uceva.submission.domain.model.ContestSnapshot;
import co.uceva.submission.domain.model.TeamSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ContestServiceDataAdapterTest {

    private static final String BASE = "http://contest-service";

    private final UUID teamId = UUID.randomUUID();
    private final UUID contestId = UUID.randomUUID();
    private final UUID userA = UUID.randomUUID();
    private final UUID userB = UUID.randomUUID();
    private final UUID problemId = UUID.randomUUID();

    private MockRestServiceServer server;
    private ContestServiceDataAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new ContestServiceDataAdapter(builder.build());
    }

    @Test
    void shouldMapATeamIgnoringUnknownFields() {
        server.expect(requestTo(BASE + "/api/v1/teams/" + teamId)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id":"%s","contestId":"%s","captainId":"%s","teamName":"x","score":3,"penaltyTime":10,
                         "createdAt":"2025-01-01T00:00:00Z",
                         "members":[{"id":"%s","teamId":"%s","userId":"%s","registeredAt":"2025-01-01T00:00:00Z"},
                                    {"id":"%s","teamId":"%s","userId":"%s","registeredAt":"2025-01-01T00:00:00Z"}]}
                        """.formatted(teamId, contestId, userA, UUID.randomUUID(), teamId, userA,
                        UUID.randomUUID(), teamId, userB), MediaType.APPLICATION_JSON));

        Optional<TeamSnapshot> team = adapter.findTeam(teamId);

        assertThat(team).isPresent();
        assertThat(team.get().contestId()).isEqualTo(contestId);
        assertThat(team.get().memberUserIds()).containsExactly(userA, userB);
        server.verify();
    }

    @Test
    void shouldMapAContest() {
        server.expect(requestTo(BASE + "/api/v1/contests/" + contestId))
                .andRespond(withSuccess("""
                        {"id":"%s","createdBy":"%s","title":"t","description":"d",
                         "startTime":"2025-06-01T10:00:00Z","endTime":"2025-06-01T14:00:00Z",
                         "status":"ACTIVE","createdAt":"2025-01-01T00:00:00Z"}
                        """.formatted(contestId, userA), MediaType.APPLICATION_JSON));

        Optional<ContestSnapshot> contest = adapter.findContest(contestId);

        assertThat(contest).isPresent();
        assertThat(contest.get().startTime()).isEqualTo(Instant.parse("2025-06-01T10:00:00Z"));
        assertThat(contest.get().endTime()).isEqualTo(Instant.parse("2025-06-01T14:00:00Z"));
    }

    @Test
    void shouldMapTheProblemIdsOfAContest() {
        server.expect(requestTo(BASE + "/api/v1/contests/" + contestId + "/problems"))
                .andRespond(withSuccess("""
                        [{"id":"%s","contestId":"%s","problemId":"%s","alias":"A","orderIndex":0,"points":100,
                          "penalty":20,"balloonColor":"red"}]
                        """.formatted(UUID.randomUUID(), contestId, problemId), MediaType.APPLICATION_JSON));

        assertThat(adapter.findContestProblemIds(contestId)).containsExactly(problemId);
    }

    @Test
    void shouldReturnEmptyWhenTheTeamOrContestIsNotFound() {
        server.expect(requestTo(BASE + "/api/v1/teams/" + teamId)).andRespond(withStatus(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON).body("{\"message\":\"no existe\"}"));
        server.expect(requestTo(BASE + "/api/v1/contests/" + contestId)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(adapter.findTeam(teamId)).isEmpty();
        assertThat(adapter.findContest(contestId)).isEmpty();
    }

    @Test
    void shouldReturnAnEmptyListWhenTheContestOfTheProblemsIsNotFound() {
        server.expect(requestTo(BASE + "/api/v1/contests/" + contestId + "/problems"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(adapter.findContestProblemIds(contestId)).isEmpty();
    }

    @Test
    void shouldReportUnavailableOnServerError() {
        server.expect(requestTo(BASE + "/api/v1/teams/" + teamId))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> adapter.findTeam(teamId)).isInstanceOf(ContestServiceUnavailableException.class);
    }

    @Test
    void shouldReportUnavailableOnConnectionFailure() {
        server.expect(requestTo(BASE + "/api/v1/contests/" + contestId))
                .andRespond((request) -> { throw new java.io.IOException("connection refused"); });

        assertThatThrownBy(() -> adapter.findContest(contestId)).isInstanceOf(ContestServiceUnavailableException.class);
    }

    @Test
    void shouldReportUnavailableOnAMalformedBody() {
        server.expect(requestTo(BASE + "/api/v1/contests/" + contestId))
                .andRespond(withSuccess("<html>", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.findContest(contestId)).isInstanceOf(ContestServiceUnavailableException.class);
    }
}
