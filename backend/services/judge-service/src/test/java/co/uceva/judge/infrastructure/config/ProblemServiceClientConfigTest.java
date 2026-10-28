package co.uceva.judge.infrastructure.config;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.sun.net.httpserver.HttpServer;

import co.uceva.judge.infrastructure.security.ServiceTokenProvider;
import co.uceva.shared.infrastructure.security.AuthenticatedUser;
import co.uceva.shared.infrastructure.security.JwtTokenValidator;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemServiceClientConfigTest {

    private static final String SECRET = "una-clave-compartida-de-al-menos-32-bytes!!";
    private static final String ISSUER = "goslint-judge";

    @Test
    void shouldSignEveryRequestWithServiceToken() throws IOException {
        UUID serviceId = UUID.randomUUID();
        AtomicReference<String> authorization = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/ping", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            ServiceTokenProvider provider = new ServiceTokenProvider(SECRET, ISSUER, serviceId, 300);
            RestClient client = new ProblemServiceClientConfig().problemServiceRestClient(RestClient.builder(),
                    provider, "http://localhost:" + server.getAddress().getPort(), 1_000, 1_000);

            client.get().uri("/ping").retrieve().body(String.class);

            assertThat(authorization.get()).startsWith("Bearer ");
            AuthenticatedUser user = new JwtTokenValidator(SECRET, ISSUER).validate(authorization.get().substring(7));
            assertThat(user.role()).isEqualTo("SERVICE");
            assertThat(user.userId()).isEqualTo(serviceId);
        } finally {
            server.stop(0);
        }
    }
}
