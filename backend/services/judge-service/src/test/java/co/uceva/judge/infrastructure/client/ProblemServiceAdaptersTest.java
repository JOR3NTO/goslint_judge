package co.uceva.judge.infrastructure.client;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import co.uceva.judge.application.port.out.ProblemLimits;
import co.uceva.judge.domain.exception.TestCasesNotFoundException;
import co.uceva.judge.domain.model.TestCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ProblemServiceAdaptersTest {

    private static final String BASE = "http://problem-service";

    private final UUID problemId = UUID.randomUUID();
    private MockRestServiceServer server;
    private ProblemServiceTestCaseRepositoryAdapter testCases;
    private ProblemServiceLimitsAdapter limits;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE).defaultHeader("Authorization", "Bearer t");
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();
        testCases = new ProblemServiceTestCaseRepositoryAdapter(client);
        limits = new ProblemServiceLimitsAdapter(client);
    }

    @Test
    void shouldReturnTestCasesSortedByOrderIndex() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        server.expect(requestTo(BASE + "/api/v1/problems/test-cases/" + problemId + "/all"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer t"))
                .andRespond(withSuccess("""
                        [{"id":"%s","input":"b","expectedOutput":"2","orderIndex":1,"isSample":false,"extra":1},
                         {"id":"%s","input":"a","expectedOutput":"1","orderIndex":0,"isSample":true}]
                        """.formatted(second, first), MediaType.APPLICATION_JSON));

        List<TestCase> result = testCases.findAllByProblemId(problemId);

        assertThat(result).extracting(TestCase::id).containsExactly(first, second);
        assertThat(result.get(0).input()).isEqualTo("a");
    }

    @Test
    void shouldTreatNotFoundAsMissingTestCases() {
        server.expect(requestTo(BASE + "/api/v1/problems/test-cases/" + problemId + "/all"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> testCases.findAllByProblemId(problemId))
                .isInstanceOf(TestCasesNotFoundException.class);
    }

    @Test
    void shouldTreatEmptyListAsMissingTestCases() {
        server.expect(requestTo(BASE + "/api/v1/problems/test-cases/" + problemId + "/all"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> testCases.findAllByProblemId(problemId))
                .isInstanceOf(TestCasesNotFoundException.class);
    }

    @Test
    void shouldReadProblemLimits() {
        server.expect(requestTo(BASE + "/api/v1/problems/" + problemId))
                .andRespond(withSuccess("""
                        {"id":"%s","title":"t","timeLimitMs":1000,"memoryLimitKb":8192,"difficult":1}
                        """.formatted(problemId), MediaType.APPLICATION_JSON));

        ProblemLimits result = limits.findByProblemId(problemId);

        assertThat(result.timeLimit().milliseconds()).isEqualTo(1_000);
        assertThat(result.memoryLimit().kilobytes()).isEqualTo(8_192);
    }
}
