package co.uceva.submission.infrastructure.websocket;

import co.uceva.shared.infrastructure.security.AuthenticatedUser;
import co.uceva.shared.infrastructure.security.JwtTokenValidator;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.jsonwebtoken.security.Keys.hmacShaKeyFor;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprueba que la conexión solo se acepta cuando el token la respalda.
 * <p>
 * Es la prueba que sostiene el requisito de autenticación: rechazar en el
 * handshake y no después significa que una conexión sin credenciales válidas
 * nunca llega a existir, y por tanto no puede recibir ni un solo mensaje.
 * </p>
 */
class JwtHandshakeInterceptorTest {

    private static final String SECRET = "clave-de-pruebas-solo-para-tests-0123456789";
    private static final String ISSUER = "goslint-judge";
    private static final UUID USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private JwtHandshakeInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new JwtHandshakeInterceptor(new JwtTokenValidator(SECRET, ISSUER));
    }

    @Test
    void shouldAcceptTheHandshakeAndExposeTheUserWhenTheTokenTravelsInTheQueryParam() {
        MockHttpServletRequest request = handshakeRequest();
        request.setQueryString("token=" + validToken());
        Map<String, Object> attributes = new HashMap<>();

        boolean accepted = beforeHandshake(request, new MockHttpServletResponse(), attributes);

        assertThat(accepted).isTrue();
        assertThat(attributes.get(JwtHandshakeInterceptor.AUTHENTICATED_USER_ATTRIBUTE))
                .isEqualTo(new AuthenticatedUser(USER_ID, "STUDENT"));
    }

    @Test
    void shouldAcceptTheHandshakeWhenTheTokenTravelsInTheAuthorizationHeader() {
        MockHttpServletRequest request = handshakeRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + validToken());
        Map<String, Object> attributes = new HashMap<>();

        assertThat(beforeHandshake(request, new MockHttpServletResponse(), attributes)).isTrue();
        assertThat(attributes).containsKey(JwtHandshakeInterceptor.AUTHENTICATED_USER_ATTRIBUTE);
    }

    /**
     * La vía que usa el navegador, que no permite fijar cabeceras al abrir un
     * WebSocket.
     */
    @Test
    void shouldAcceptTheHandshakeWhenTheTokenTravelsInTheSubprotocol() {
        MockHttpServletRequest request = handshakeRequest();
        request.addHeader("Sec-WebSocket-Protocol", "goslint-judge, bearer." + validToken());
        Map<String, Object> attributes = new HashMap<>();

        assertThat(beforeHandshake(request, new MockHttpServletResponse(), attributes)).isTrue();
        assertThat(attributes).containsKey(JwtHandshakeInterceptor.AUTHENTICATED_USER_ATTRIBUTE);
    }

    @Test
    void shouldRejectTheHandshakeWithUnauthorizedWhenNoTokenIsPresented() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        Map<String, Object> attributes = new HashMap<>();

        boolean accepted = beforeHandshake(handshakeRequest(), response, attributes);

        assertThat(accepted).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(attributes).isEmpty();
    }

    /** Un token firmado con otra clave es exactamente igual de inútil que ninguno. */
    @Test
    void shouldRejectTheHandshakeWhenTheTokenIsSignedWithAnotherKey() {
        MockHttpServletRequest request = handshakeRequest();
        request.setQueryString("token=" + tokenSignedWith(
                "otra-clave-completamente-distinta-0123456789", ISSUER, Instant.now().plusSeconds(300)));

        assertThat(beforeHandshake(request, new MockHttpServletResponse(), new HashMap<>())).isFalse();
    }

    @Test
    void shouldRejectTheHandshakeWhenTheTokenHasExpired() {
        MockHttpServletRequest request = handshakeRequest();
        request.setQueryString("token=" + tokenSignedWith(SECRET, ISSUER, Instant.now().minusSeconds(60)));

        assertThat(beforeHandshake(request, new MockHttpServletResponse(), new HashMap<>())).isFalse();
    }

    /** Un token válido de otro sistema no sirve para entrar en este. */
    @Test
    void shouldRejectTheHandshakeWhenTheTokenComesFromAnotherIssuer() {
        MockHttpServletRequest request = handshakeRequest();
        request.setQueryString("token=" + tokenSignedWith(
                SECRET, "otro-emisor", Instant.now().plusSeconds(300)));

        assertThat(beforeHandshake(request, new MockHttpServletResponse(), new HashMap<>())).isFalse();
    }

    @Test
    void shouldRejectTheHandshakeWhenTheTokenIsNotEvenAToken() {
        MockHttpServletRequest request = handshakeRequest();
        request.setQueryString("token=esto-no-es-un-jwt");

        assertThat(beforeHandshake(request, new MockHttpServletResponse(), new HashMap<>())).isFalse();
    }

    private boolean beforeHandshake(MockHttpServletRequest request, MockHttpServletResponse response,
            Map<String, Object> attributes) {
        return interceptor.beforeHandshake(
                new ServletServerHttpRequest(request),
                new ServletServerHttpResponse(response),
                null,
                attributes);
    }

    private MockHttpServletRequest handshakeRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws/submissions");
        request.setServerName("localhost");
        request.setServerPort(8083);
        return request;
    }

    private String validToken() {
        return tokenSignedWith(SECRET, ISSUER, Instant.now().plusSeconds(300));
    }

    private String tokenSignedWith(String secret, String issuer, Instant expiration) {
        SecretKey key = hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(USER_ID.toString())
                .issuer(issuer)
                .claim("role", "STUDENT")
                .issuedAt(Date.from(Instant.now().minusSeconds(60)))
                .expiration(Date.from(expiration))
                .signWith(key)
                .compact();
    }
}
