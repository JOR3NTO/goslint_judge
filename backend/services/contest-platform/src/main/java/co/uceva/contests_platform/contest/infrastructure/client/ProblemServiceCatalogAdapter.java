package co.uceva.contests_platform.contest.infrastructure.client;

import java.util.UUID;

import co.uceva.contests_platform.problem.application.usecase.GetProblemByIdUseCase;
import co.uceva.contests_platform.problem.domain.exception.ProblemNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import co.uceva.contests_platform.contest.domain.exception.ProblemCatalogUnavailableException;
import co.uceva.contests_platform.contest.domain.service.ProblemCatalog;

/**
 * Adaptador de {@link ProblemCatalog} que consulta el módulo {@code problem} llamando
 * directamente a su caso de uso, dentro del mismo proceso (sin HTTP).
 */
@Component
public class ProblemServiceCatalogAdapter implements ProblemCatalog {

    private final GetProblemByIdUseCase getProblemByIdUseCase;

    /**
     * @param getProblemByIdUseCase Caso de uso para consultar un problema por id.
     */
    public ProblemServiceCatalogAdapter(GetProblemByIdUseCase getProblemByIdUseCase) {
        this.getProblemByIdUseCase = getProblemByIdUseCase;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Una excepcion significa que el problema no existe.
     * </p>
     */
    @Override
    public boolean exists(UUID problemId) {
        try {
            getProblemByIdUseCase.execute(problemId);
        }catch (ProblemNotFoundException e){
            return false;
        }
        return true;
    }
}
