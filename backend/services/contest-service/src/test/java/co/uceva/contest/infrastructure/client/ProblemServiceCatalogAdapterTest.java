package co.uceva.contest.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.UUID;

import co.uceva.contest.domain.exception.ProblemCatalogUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@DisplayName("Adaptador del catálogo de problemas sobre problem-service")
class ProblemServiceCatalogAdapterTest {

    private final UUID id = UUID.randomUUID();
    private final RestClient.Builder builder = RestClient.builder().baseUrl("http://problems");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final ProblemServiceCatalogAdapter adapter = new ProblemServiceCatalogAdapter(builder.build());

    @Test
    @DisplayName("200 significa que el problema existe; 404, que no existe")
    void mapsFoundAndNotFound() {
        server.expect(requestTo("http://problems/api/v1/problems/" + id)).andRespond(withSuccess("{}", org.springframework.http.MediaType.APPLICATION_JSON));
        assertThat(adapter.exists(id)).isTrue();

        server.reset();
        server.expect(requestTo("http://problems/api/v1/problems/" + id)).andRespond(withResourceNotFound());
        assertThat(adapter.exists(id)).isFalse();
    }

    @Test
    @DisplayName("Un 5xx se informa como catálogo no disponible, no como problema inexistente")
    void mapsServerErrorToUnavailable() {
        server.expect(requestTo("http://problems/api/v1/problems/" + id)).andRespond(withServerError());

        assertThatThrownBy(() -> adapter.exists(id)).isInstanceOf(ProblemCatalogUnavailableException.class);
    }
}
