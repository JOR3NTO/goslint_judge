package co.uceva.contest.infrastructure.client;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import co.uceva.contest.domain.exception.ProblemCatalogUnavailableException;
import co.uceva.contest.domain.service.ProblemCatalog;

/**
 * Adaptador de {@link ProblemCatalog} que consulta {@code problem-service} por HTTP.
 * <p>
 * Usa {@code GET /api/v1/problems/{id}}, que es público, por lo que no necesita token.
 * </p>
 */
@Component
public class ProblemServiceCatalogAdapter implements ProblemCatalog {

    private final RestClient restClient;

    /**
     * @param problemServiceRestClient Cliente HTTP hacia {@code problem-service}.
     */
    public ProblemServiceCatalogAdapter(RestClient problemServiceRestClient) {
        this.restClient = problemServiceRestClient;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Un 404 significa que el problema no existe; cualquier otro fallo (red, tiempo
     * de espera, 5xx, 401/403) se informa como catálogo no disponible para no
     * confundirlo con un problema inexistente.
     * </p>
     */
    @Override
    public boolean exists(UUID problemId) {
        try {
            restClient.get()
                    .uri("/api/v1/problems/{id}", problemId)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (RestClientException e) {
            throw new ProblemCatalogUnavailableException("No se pudo consultar problem-service.", e);
        }
    }
}
