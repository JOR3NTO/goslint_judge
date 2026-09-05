package co.uceva.submission.infrastructure.websocket;

import co.uceva.submission.application.port.out.SubmissionStatusNotifier;
import co.uceva.submission.domain.model.Submission;
import co.uceva.submission.infrastructure.websocket.dto.SubmissionStatusEventDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Adaptador de infraestructura que empuja el estado de un envío por WebSocket a
 * los usuarios con derecho a verlo.
 * <p>
 * Es solo el canal: recibe un envío ya persistido y una lista de destinatarios ya
 * decidida, y se limita a serializar y enviar. No consulta la base de datos, no
 * decide quién recibe qué y no participa en el flujo de evaluación, de modo que
 * el juez y el WebSocket permanecen desacoplados.
 * </p>
 */
@Component
public class WebSocketSubmissionStatusNotifierAdapter implements SubmissionStatusNotifier {

    private static final Logger log = LoggerFactory.getLogger(WebSocketSubmissionStatusNotifierAdapter.class);

    private final WebSocketSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param sessionRegistry Índice de conexiones abiertas por usuario.
     * @param objectMapper    Mapper autoconfigurado por Spring Boot, con soporte para {@code Instant}.
     */
    public WebSocketSubmissionStatusNotifierAdapter(WebSocketSessionRegistry sessionRegistry,
            ObjectMapper objectMapper) {
        this.sessionRegistry = sessionRegistry;
        this.objectMapper = objectMapper;
    }

    /**
     * Serializa el estado del envío una sola vez y lo envía a todas las conexiones
     * abiertas de los destinatarios.
     * <p>
     * Un destinatario sin conexión abierta simplemente no recibe nada, y un fallo
     * al escribir en una conexión concreta no impide el envío a las demás: el
     * veredicto ya está registrado, así que fallar aquí solo retrasa la vista, no
     * la pierde.
     * </p>
     *
     * @param submission       Envío cuyo estado se comunica.
     * @param recipientUserIds Usuarios con derecho a recibir esta actualización.
     */
    @Override
    public void notifyStatusChanged(Submission submission, List<UUID> recipientUserIds) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(toEvent(submission));
        } catch (JsonProcessingException e) {
            log.warn("No se pudo serializar el estado del envío {}; no se notifica.", submission.getId(), e);
            return;
        }

        TextMessage message = new TextMessage(payload);
        recipientUserIds.forEach(userId -> sendTo(userId, message, submission.getId()));
    }

    /**
     * Envía el mensaje a todas las conexiones abiertas de un usuario.
     *
     * @param userId       Destinatario.
     * @param message      Mensaje ya serializado.
     * @param submissionId Envío notificado, solo para trazabilidad.
     */
    private void sendTo(UUID userId, TextMessage message, UUID submissionId) {
        for (WebSocketSession session : sessionRegistry.sessionsOf(userId)) {
            if (!session.isOpen()) {
                continue;
            }
            try {
                // Una sesión no admite escrituras concurrentes: dos notificaciones
                // simultáneas al mismo usuario podrían entrelazar sus fragmentos.
                synchronized (session) {
                    session.sendMessage(message);
                }
            } catch (IOException | IllegalStateException e) {
                log.warn("No se pudo entregar el estado del envío {} al usuario {} por la conexión {}.",
                        submissionId, userId, session.getId(), e);
            }
        }
    }

    /**
     * Convierte el envío en el mensaje que viaja por el canal.
     *
     * @param submission Envío cuyo estado se comunica.
     * @return DTO listo para serializar.
     */
    private SubmissionStatusEventDTO toEvent(Submission submission) {
        return new SubmissionStatusEventDTO(
                SubmissionStatusEventDTO.SUBMISSION_STATUS_UPDATED,
                submission.getId(),
                submission.getProblemId(),
                submission.getTeamId(),
                submission.getStatus(),
                submission.getVerdict(),
                submission.getExecutionTimeMs(),
                submission.getMemoryUsedKb(),
                Instant.now()
        );
    }
}
