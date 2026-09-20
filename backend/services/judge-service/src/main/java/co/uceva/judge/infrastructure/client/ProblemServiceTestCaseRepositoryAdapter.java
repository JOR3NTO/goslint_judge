package co.uceva.judge.infrastructure.client;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import co.uceva.judge.domain.exception.TestCasesNotFoundException;
import co.uceva.judge.domain.model.TestCase;
import co.uceva.judge.domain.repository.TestCaseRepository;
import co.uceva.judge.infrastructure.client.dto.TestCaseResponse;

/**
 * Adaptador de {@link TestCaseRepository} que obtiene los casos de prueba,
 * incluidos los privados, desde {@code problem-service} por HTTP.
 */
@Component
public class ProblemServiceTestCaseRepositoryAdapter implements TestCaseRepository {

    private final RestClient restClient;

    /**
     * @param problemServiceRestClient Cliente HTTP hacia {@code problem-service}.
     */
    public ProblemServiceTestCaseRepositoryAdapter(RestClient problemServiceRestClient) {
        this.restClient = problemServiceRestClient;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Un {@code 404} o una lista vacía se traducen a
     * {@link TestCasesNotFoundException}. Cualquier otro fallo HTTP se propaga
     * como excepción de cliente, tratada como transitoria por el reintento.
     * </p>
     */
    @Override
    public List<TestCase> findAllByProblemId(UUID problemId) {
        List<TestCaseResponse> body = restClient.get()
                .uri("/api/v1/problems/test-cases/{problemId}/all", problemId)
                .retrieve()
                .onStatus(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND), (request, response) -> {
                    throw new TestCasesNotFoundException(problemId);
                })
                .body(new ParameterizedTypeReference<List<TestCaseResponse>>() {});

        if (body == null || body.isEmpty()) {
            throw new TestCasesNotFoundException(problemId);
        }
        return body.stream()
                .sorted(Comparator.comparingInt(TestCaseResponse::orderIndex))
                .map(dto -> new TestCase(dto.id(), dto.input() == null ? "" : dto.input(),
                        dto.expectedOutput(), dto.orderIndex()))
                .toList();
    }
}
