package co.uceva.judge.infrastructure.client;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import co.uceva.judge.application.port.out.ProblemLimits;
import co.uceva.judge.application.port.out.ProblemLimitsPort;
import co.uceva.judge.domain.valueobject.MemoryLimit;
import co.uceva.judge.domain.valueobject.TimeLimit;
import co.uceva.judge.infrastructure.client.dto.ProblemResponse;

/**
 * Adaptador de {@link ProblemLimitsPort} que lee los límites del problema desde
 * {@code problem-service} por HTTP.
 */
@Component
public class ProblemServiceLimitsAdapter implements ProblemLimitsPort {

    private final RestClient restClient;

    /**
     * @param problemServiceRestClient Cliente HTTP hacia {@code problem-service}.
     */
    public ProblemServiceLimitsAdapter(RestClient problemServiceRestClient) {
        this.restClient = problemServiceRestClient;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Los fallos HTTP se propagan como excepción de cliente, tratada como
     * transitoria por el reintento de la mensajería.
     * </p>
     */
    @Override
    public ProblemLimits findByProblemId(UUID problemId) {
        ProblemResponse problem = restClient.get()
                .uri("/api/v1/problems/{id}", problemId)
                .retrieve()
                .body(ProblemResponse.class);

        if (problem == null) {
            throw new IllegalStateException("problem-service devolvió una respuesta vacía para el problema " + problemId);
        }
        return new ProblemLimits(new TimeLimit(problem.timeLimitMs()), new MemoryLimit(problem.memoryLimitKb()));
    }
}
