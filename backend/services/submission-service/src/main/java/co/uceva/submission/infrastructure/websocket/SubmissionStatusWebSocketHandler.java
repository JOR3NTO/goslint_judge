package co.uceva.submission.infrastructure.websocket;

import co.uceva.shared.infrastructure.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.util.List;

/**
 * Atiende el ciclo de vida de las conexiones que siguen el estado de los envíos.
 * <p>
 * El canal es de un solo sentido: el servidor empuja y el cliente escucha. Las
 * únicas acciones válidas del cliente son conectarse y desconectarse, y por eso
 * aquí solo hay altas y bajas en el registro de sesiones. No existe ningún
 * manejador que interprete lo que el cliente envíe, de modo que no hay superficie
 * por la que un mensaje entrante pueda desencadenar una acción.
 * </p>
 * <p>
 * Los mensajes que llegan del cliente se descartan sin leerlos, en lugar de
 * cerrar la conexión: un cliente que envía algo por error o un intermediario que
 * inyecta un latido no deberían costarle al estudiante el canal por el que espera
 * su veredicto. Se descartan igualmente, así que el efecto sobre el servidor es
 * ninguno.
 * </p>
 */
public class SubmissionStatusWebSocketHandler extends AbstractWebSocketHandler implements SubProtocolCapable {

    private static final Logger log = LoggerFactory.getLogger(SubmissionStatusWebSocketHandler.class);

    private final WebSocketSessionRegistry sessionRegistry;
    private final String subProtocol;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param sessionRegistry Índice de conexiones abiertas por usuario.
     * @param subProtocol     Subprotocolo que el servidor confirma al cliente.
     */
    public SubmissionStatusWebSocketHandler(WebSocketSessionRegistry sessionRegistry, String subProtocol) {
        this.sessionRegistry = sessionRegistry;
        this.subProtocol = subProtocol;
    }

    /**
     * Da de alta la conexión a nombre del usuario que se autenticó en el handshake.
     *
     * @param session Conexión recién abierta.
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        AuthenticatedUser user = authenticatedUserOf(session);
        sessionRegistry.register(user.userId(), session);
        log.info("Usuario {} conectado al canal de estado de envíos.", user.userId());
    }

    /**
     * Da de baja la conexión cerrada.
     *
     * @param session Conexión que se cierra.
     * @param status  Motivo del cierre.
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        AuthenticatedUser user = authenticatedUserOf(session);
        sessionRegistry.unregister(user.userId(), session);
        log.info("Usuario {} desconectado del canal de estado de envíos ({}).", user.userId(), status);
    }

    /**
     * Descarta cualquier mensaje entrante sin interpretarlo.
     * <p>
     * Sobrescribe el despacho por tipo de la clase base a propósito: así ninguna
     * subclase futura puede introducir lógica de negocio limitándose a implementar
     * {@code handleTextMessage}.
     * </p>
     *
     * @param session Conexión por la que llegó el mensaje.
     * @param message Mensaje recibido, que no se procesa.
     */
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        log.debug("Mensaje entrante descartado en la conexión {}: el canal es de solo notificación.",
                session.getId());
    }

    /**
     * Cierra la conexión ante un error de transporte.
     *
     * @param session   Conexión afectada.
     * @param exception Error ocurrido en el transporte.
     * @throws Exception Si la conexión no puede cerrarse.
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("Error de transporte en la conexión {}; se cierra.", session.getId(), exception);
        session.close(CloseStatus.SERVER_ERROR);
    }

    /**
     * Subprotocolos que el servidor acepta confirmar.
     * <p>
     * El cliente de navegador anuncia dos: este, fijo, y el que transporta su
     * token. El servidor confirma solo este, porque el protocolo exige devolver
     * uno de los ofrecidos y devolver el que contiene el token lo expondría en la
     * respuesta.
     * </p>
     *
     * @return Lista con el único subprotocolo soportado.
     */
    @Override
    public List<String> getSubProtocols() {
        return List.of(subProtocol);
    }

    /**
     * Recupera la identidad que el interceptor de handshake dejó en la sesión.
     * <p>
     * Nunca es {@code null}: una conexión sin usuario autenticado no llega a
     * abrirse, porque el interceptor la rechaza durante el handshake.
     * </p>
     *
     * @param session Conexión abierta.
     * @return Identidad del usuario dueño de la conexión.
     */
    private AuthenticatedUser authenticatedUserOf(WebSocketSession session) {
        return (AuthenticatedUser) session.getAttributes()
                .get(JwtHandshakeInterceptor.AUTHENTICATED_USER_ATTRIBUTE);
    }
}
