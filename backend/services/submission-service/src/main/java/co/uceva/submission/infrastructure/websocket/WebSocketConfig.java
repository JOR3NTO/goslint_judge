package co.uceva.submission.infrastructure.websocket;

import co.uceva.shared.infrastructure.security.JwtTokenValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Configuración del canal WebSocket por el que se empujan los cambios de estado
 * de los envíos.
 * <p>
 * Se usa el WebSocket a pelo, sin STOMP ni un broker de mensajes por encima. Es
 * la elección que corresponde a un canal de solo notificación: STOMP existe
 * precisamente para que el cliente pueda suscribirse y publicar, y nada de eso
 * debe ser posible aquí.
 * </p>
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketSessionRegistry sessionRegistry;
    private final JwtTokenValidator jwtTokenValidator;
    private final String path;
    private final String subProtocol;
    private final String[] allowedOrigins;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param sessionRegistry   Índice de conexiones abiertas por usuario.
     * @param jwtTokenValidator Validador compartido de tokens JWT.
     * @param path              Ruta en la que se expone el canal.
     * @param subProtocol       Subprotocolo que el servidor confirma al cliente.
     * @param allowedOrigins    Orígenes autorizados a abrir la conexión.
     */
    public WebSocketConfig(WebSocketSessionRegistry sessionRegistry,
            JwtTokenValidator jwtTokenValidator,
            @Value("${app.websocket.submission.path}") String path,
            @Value("${app.websocket.submission.sub-protocol}") String subProtocol,
            @Value("${app.websocket.allowed-origins}") String[] allowedOrigins) {
        this.sessionRegistry = sessionRegistry;
        this.jwtTokenValidator = jwtTokenValidator;
        this.path = path;
        this.subProtocol = subProtocol;
        this.allowedOrigins = allowedOrigins;
    }

    /**
     * Publica el handler en su ruta, con la autenticación enganchada al handshake.
     * <p>
     * El interceptor se registra antes que nada: es lo que hace que una conexión
     * sin token válido se rechace con {@code 401} en el propio handshake, en vez
     * de abrirse y cerrarse después.
     * </p>
     *
     * @param registry Registro de handlers WebSocket.
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(submissionStatusWebSocketHandler(), path)
                .addInterceptors(new JwtHandshakeInterceptor(jwtTokenValidator))
                .setAllowedOriginPatterns(allowedOrigins);
    }

    /**
     * Handler que atiende el ciclo de vida de las conexiones.
     *
     * @return Handler del canal de estado de envíos.
     */
    @Bean
    public SubmissionStatusWebSocketHandler submissionStatusWebSocketHandler() {
        return new SubmissionStatusWebSocketHandler(sessionRegistry, subProtocol);
    }
}
