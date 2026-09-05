package co.uceva.submission.infrastructure.websocket;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WebSocketSessionRegistryTest {

    private static final UUID USUARIO = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID OTRO_USUARIO = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private final WebSocketSessionRegistry registry = new WebSocketSessionRegistry();

    @Test
    void shouldKeepEverySessionOfTheSameUser() {
        WebSocketSession primeraPestana = mock(WebSocketSession.class);
        WebSocketSession segundaPestana = mock(WebSocketSession.class);

        registry.register(USUARIO, primeraPestana);
        registry.register(USUARIO, segundaPestana);

        assertThat(registry.sessionsOf(USUARIO)).containsExactlyInAnyOrder(primeraPestana, segundaPestana);
    }

    /** Cada usuario ve solo lo suyo, empezando por no compartir conexiones. */
    @Test
    void shouldKeepUsersSessionsApart() {
        WebSocketSession sesionPropia = mock(WebSocketSession.class);
        WebSocketSession sesionAjena = mock(WebSocketSession.class);

        registry.register(USUARIO, sesionPropia);
        registry.register(OTRO_USUARIO, sesionAjena);

        assertThat(registry.sessionsOf(USUARIO)).containsExactly(sesionPropia);
        assertThat(registry.sessionsOf(OTRO_USUARIO)).containsExactly(sesionAjena);
    }

    @Test
    void shouldForgetASessionOnceClosed() {
        WebSocketSession session = mock(WebSocketSession.class);
        registry.register(USUARIO, session);

        registry.unregister(USUARIO, session);

        assertThat(registry.sessionsOf(USUARIO)).isEmpty();
    }

    /** Una desconexión de la que no hay constancia no puede tumbar nada. */
    @Test
    void shouldTolerateUnregisteringAnUnknownSession() {
        registry.unregister(USUARIO, mock(WebSocketSession.class));

        assertThat(registry.sessionsOf(USUARIO)).isEmpty();
    }

    @Test
    void shouldReturnNoSessionsForAUserThatNeverConnected() {
        assertThat(registry.sessionsOf(UUID.randomUUID())).isEmpty();
    }
}
