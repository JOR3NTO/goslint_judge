package co.uceva.submission.infrastructure.client;

import co.uceva.submission.application.exception.ContestServiceUnavailableException;
import co.uceva.submission.application.port.out.ContestDataPort;
import co.uceva.submission.domain.model.ContestSnapshot;
import co.uceva.submission.domain.model.TeamSnapshot;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de {@link ContestDataPort} que consulta los endpoints públicos de
 * {@code contest-service} mediante {@link RestClient}.
 * <p>
 * Distingue dos clases de resultado: un {@code 404} es una respuesta válida
 * ("no existe") y se traduce en {@code Optional.empty()} (o lista vacía); cualquier
 * otro fallo, ya sea un estado de error o un problema de red, se traduce en
 * {@link ContestServiceUnavailableException}, porque en ese caso no se sabe si el
 * recurso existe y rechazar el envío como si no existiera sería engañoso.
 * </p>
 */
@Component
public class ContestServiceDataAdapter implements ContestDataPort {

    private final RestClient restClient;

    /** @param restClient Cliente HTTP configurado contra {@code contest-service}. */
    public ContestServiceDataAdapter(@Qualifier("contestServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<TeamSnapshot> findTeam(UUID teamId) {
        return fetch("/api/v1/teams/{id}", teamId, new ParameterizedTypeReference<TeamBody>() {})
                .map(body -> new TeamSnapshot(
                        body.id(),
                        body.contestId(),
                        body.members() == null ? List.of()
                                : body.members().stream().map(MemberBody::userId).toList()));
    }

    @Override
    public Optional<ContestSnapshot> findContest(UUID contestId) {
        return fetch("/api/v1/contests/{id}", contestId, new ParameterizedTypeReference<ContestBody>() {})
                .map(body -> new ContestSnapshot(body.id(), body.startTime(), body.endTime()));
    }

    @Override
    public List<UUID> findContestProblemIds(UUID contestId) {
        return fetch("/api/v1/contests/{id}/problems", contestId,
                new ParameterizedTypeReference<List<ContestProblemBody>>() {})
                .map(list -> list.stream().map(ContestProblemBody::problemId).toList())
                .orElse(List.of());
    }

    /**
     * Ejecuta un GET y deserializa la respuesta, traduciendo 404 a vacío y
     * cualquier otro fallo a {@link ContestServiceUnavailableException}.
     */
    private <T> Optional<T> fetch(String uri, UUID id, ParameterizedTypeReference<T> type) {
        try {
            return restClient.get()
                    .uri(uri, id)
                    .exchange((request, response) -> {
                        HttpStatusCode status = response.getStatusCode();
                        if (status.value() == 404) {
                            return Optional.<T>empty();
                        }
                        if (!status.is2xxSuccessful()) {
                            throw new ContestServiceUnavailableException(
                                    "respuesta inesperada " + status.value() + " en " + uri, null);
                        }
                        return Optional.ofNullable(response.bodyTo(type));
                    });
        } catch (RestClientException ex) {
            throw new ContestServiceUnavailableException(ex.getMessage(), ex);
        }
    }

    // --- DTOs internos: solo los campos necesarios; el resto se ignora ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TeamBody(UUID id, UUID contestId, List<MemberBody> members) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MemberBody(UUID userId) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ContestBody(UUID id, Instant startTime, Instant endTime) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ContestProblemBody(UUID problemId) {}
}
