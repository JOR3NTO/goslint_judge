package co.uceva.submission.infrastructure.websocket;

import co.uceva.shared.infrastructure.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Comprueba el ciclo de vida de la conexión y, sobre todo, que el canal sea
 * realmente de un solo sentido.
 */
class SubmissionStatusWebSocketHandlerTest {

    private static final UUID USUARIO = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private WebSocketSessionRegistry sessionRegistry;
    private SubmissionStatusWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        sessionRegistry = new WebSocketSessionRegistry();
        handler = new SubmissionStatusWebSocketHandler(sessionRegistry, "goslint-judge");
    }

    @Test
    void shouldRegisterTheConnectionUnderTheAuthenticatedUser() {
        WebSocketSession session = authenticatedSession();

        handler.afterConnectionEstablished(session);

        assertThat(sessionRegistry.sessionsOf(USUARIO)).containsExactly(session);
    }

    @Test
    void shouldUnregisterTheConnectionOnClose() {
        WebSocketSession session = authenticatedSession();
        handler.afterConnectionEstablished(session);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        assertThat(sessionRegistry.sessionsOf(USUARIO)).isEmpty();
    }

    /**
     * El requisito central del canal: nada de lo que el cliente envíe se procesa.
     * La conexión se mantiene abierta y el registro queda intacto.
     */
    @Test
    void shouldIgnoreEverythingTheClientSends() {
        WebSocketSession session = authenticatedSession();
        handler.afterConnectionEstablished(session);

        assertThatCode(() -> {
            handler.handleMessage(session, new TextMessage("{\"action\":\"resubmit\"}"));
            handler.handleMessage(session, new TextMessage("{\"subscribeTo\":\"otro-envio\"}"));
            handler.handleMessage(session, new BinaryMessage(new byte[]{1, 2, 3}));
        }).doesNotThrowAnyException();

        assertThat(sessionRegistry.sessionsOf(USUARIO)).containsExactly(session);
    }

    /**
     * El servidor confirma un subprotocolo fijo y nunca el que transporta el token,
     * que acabaría reflejado en la respuesta del handshake.
     */
    @Test
    void shouldOnlyAdvertiseTheFixedSubProtocol() {
        assertThat(handler.getSubProtocols()).containsExactly("goslint-judge");
    }

    private WebSocketSession authenticatedSession() {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(JwtHandshakeInterceptor.AUTHENTICATED_USER_ATTRIBUTE,
                new AuthenticatedUser(USUARIO, "STUDENT"));
        when(session.getAttributes()).thenReturn(attributes);
        return session;
    }
}
