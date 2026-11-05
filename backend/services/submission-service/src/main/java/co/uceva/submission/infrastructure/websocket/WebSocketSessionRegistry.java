package co.uceva.submission.infrastructure.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Índice en memoria de las conexiones WebSocket abiertas, agrupadas por usuario.
 * <p>
 * Es la pieza que asocia cada conexión a la identidad que presentó en el
 * handshake, y por tanto la que permite empujar una actualización a una persona
 * concreta sin difundirla a todo el mundo. Un usuario puede tener varias sesiones
 * a la vez (varias pestañas o dispositivos), de ahí que cada entrada guarde un
 * conjunto y no una sola sesión.
 * </p>
 * <p>
 * El estado es deliberadamente volátil: una conexión no sobrevive al reinicio del
 * servicio ni se comparte entre instancias, y no tiene por qué hacerlo. El estado
 * duradero del envío vive en la base de datos; esto es solo el mapa de a quién se
 * le puede avisar ahora mismo.
 * </p>
 */
@Component
public class WebSocketSessionRegistry {

    private static final Logger log = LoggerFactory.getLogger(WebSocketSessionRegistry.class);

    /** Sesiones abiertas indexadas por el usuario autenticado en el handshake. */
    private final Map<UUID, Set<WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();

    /**
     * Registra una conexión recién establecida a nombre de su usuario.
     *
     * @param userId  Usuario autenticado durante el handshake.
     * @param session Conexión abierta.
     */
    public void register(UUID userId, WebSocketSession session) {
        sessionsByUser
                .computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet())
                .add(session);
        log.debug("Conexión {} registrada para el usuario {}.", session.getId(), userId);
    }

    /**
     * Da de baja una conexión cerrada.
     * <p>
     * La entrada del usuario se elimina cuando se queda sin sesiones, para que el
     * mapa no crezca indefinidamente con usuarios que hace tiempo se desconectaron.
     * </p>
     *
     * @param userId  Usuario dueño de la conexión.
     * @param session Conexión que se cierra.
     */
    public void unregister(UUID userId, WebSocketSession session) {
        sessionsByUser.computeIfPresent(userId, (key, sessions) -> {
            sessions.remove(session);
            return sessions.isEmpty() ? null : sessions;
        });
        log.debug("Conexión {} dada de baja para el usuario {}.", session.getId(), userId);
    }

    /**
     * Recupera las conexiones abiertas de un usuario.
     *
     * @param userId Usuario del que se quieren las conexiones.
     * @return Conexiones abiertas; colección vacía si no tiene ninguna.
     */
    public Collection<WebSocketSession> sessionsOf(UUID userId) {
        return sessionsByUser.getOrDefault(userId, Set.of());
    }
}
