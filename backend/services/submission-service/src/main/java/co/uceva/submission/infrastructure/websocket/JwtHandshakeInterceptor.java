package co.uceva.submission.infrastructure.websocket;

import co.uceva.shared.infrastructure.security.AuthenticatedUser;
import co.uceva.shared.infrastructure.security.InvalidTokenException;
import co.uceva.shared.infrastructure.security.JwtTokenValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

/**
 * Autentica la conexión WebSocket durante el handshake, antes de aceptarla.
 * <p>
 * Devolver {@code false} aquí hace que el servidor responda al handshake HTTP con
 * un {@code 401} y no llegue a abrir el canal. Es distinto de aceptar la conexión
 * y cerrarla después: una conexión que nunca se abre no puede recibir ni un solo
 * mensaje, ni siquiera por una condición de carrera.
 * </p>
 * <p>
 * El token se busca en el subprotocolo, en la cabecera {@code Authorization} y,
 * por último, en el parámetro de consulta {@code token}. Los tres existen porque
 * la API {@code WebSocket} de los navegadores no permite fijar cabeceras: el
 * subprotocolo es el mecanismo recomendado para un cliente web, la cabecera queda
 * para clientes que sí pueden enviarla, y el parámetro de consulta es el recurso
 * de compatibilidad.
 * </p>
 * <p>
 * El parámetro de consulta tiene una pega conocida: las URL suelen acabar en los
 * registros de acceso de proxies y servidores, con el token dentro. Por eso los
 * tokens deben ser de vida corta y, en producción, la conexión ir siempre sobre
 * TLS ({@code wss://}).
 * </p>
 */
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtHandshakeInterceptor.class);

    /** Clave con la que la identidad autenticada viaja hasta el handler. */
    public static final String AUTHENTICATED_USER_ATTRIBUTE = "authenticatedUser";

    /** Prefijo del subprotocolo que transporta el token en clientes de navegador. */
    private static final String TOKEN_SUBPROTOCOL_PREFIX = "bearer.";

    /** Prefijo del esquema de autorización HTTP portador. */
    private static final String BEARER_PREFIX = "Bearer ";

    /** Parámetro de consulta que transporta el token cuando no hay otra vía. */
    private static final String TOKEN_QUERY_PARAM = "token";

    /** Cabecera con la que el cliente anuncia los subprotocolos que ofrece. */
    private static final String SEC_WEBSOCKET_PROTOCOL_HEADER = "Sec-WebSocket-Protocol";

    private final JwtTokenValidator jwtTokenValidator;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param jwtTokenValidator Validador compartido de tokens JWT.
     */
    public JwtHandshakeInterceptor(JwtTokenValidator jwtTokenValidator) {
        this.jwtTokenValidator = jwtTokenValidator;
    }

    /**
     * Valida el token presentado y, si es correcto, deja la identidad disponible
     * para el handler.
     *
     * @param request    Petición de handshake.
     * @param response   Respuesta de handshake.
     * @param wsHandler  Handler que atenderá la conexión.
     * @param attributes Atributos que se copiarán a la sesión WebSocket.
     * @return {@code true} si la conexión puede aceptarse; {@code false} para rechazarla.
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes) {
        try {
            AuthenticatedUser user = jwtTokenValidator.validate(extractToken(request));
            attributes.put(AUTHENTICATED_USER_ATTRIBUTE, user);
            return true;
        } catch (InvalidTokenException e) {
            // El motivo se registra pero no se devuelve: detallar por qué falla un
            // token solo ayudaría a quien está intentando adivinar uno válido.
            log.debug("Handshake WebSocket rechazado: {}", e.getMessage());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    /**
     * No hay nada que hacer una vez completado el handshake.
     *
     * @param request   Petición de handshake.
     * @param response  Respuesta de handshake.
     * @param wsHandler Handler que atenderá la conexión.
     * @param exception Excepción ocurrida durante el handshake, si la hubo.
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception exception) {
        // Sin acciones posteriores al handshake.
    }

    /**
     * Localiza el token en la petición de handshake.
     *
     * @param request Petición de handshake.
     * @return El token encontrado, o {@code null} si la petición no trae ninguno.
     */
    private String extractToken(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();

        List<String> subprotocols = headers.getOrEmpty(SEC_WEBSOCKET_PROTOCOL_HEADER);
        for (String subprotocol : subprotocols) {
            // Un mismo encabezado puede listar varios subprotocolos separados por comas.
            for (String candidate : subprotocol.split(",")) {
                String trimmed = candidate.trim();
                if (trimmed.startsWith(TOKEN_SUBPROTOCOL_PREFIX)) {
                    return trimmed.substring(TOKEN_SUBPROTOCOL_PREFIX.length());
                }
            }
        }

        String authorization = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length());
        }

        return UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst(TOKEN_QUERY_PARAM);
    }
}
