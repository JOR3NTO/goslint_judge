package co.uceva.submission.infrastructure.messaging;

import co.uceva.submission.application.port.out.SubmissionEventPublisher;
import co.uceva.submission.domain.model.Submission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adaptador de infraestructura que implementa {@link SubmissionEventPublisher}
 * sin publicar realmente el evento.
 * <p>
 * Solo se activa cuando la mensajería está desactivada mediante
 * {@code app.messaging.enabled=false}, lo que permite levantar el servicio y
 * ejecutar las pruebas sin necesidad de un broker en marcha. En ejecución normal
 * el bean activo es {@link RabbitSubmissionEventPublisherAdapter}.
 * </p>
 */
@Component
@ConditionalOnProperty(prefix = "app.messaging", name = "enabled", havingValue = "false")
public class NoOpSubmissionEventPublisherAdapter implements SubmissionEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NoOpSubmissionEventPublisherAdapter.class);

    /**
     * Da la entrega por confirmada sin contactar con ningún broker.
     * <p>
     * No lanza excepción a propósito: con la mensajería desactivada, dejar los
     * envíos atrapados en estado pendiente y reintentándolos indefinidamente no
     * aportaría nada.
     * </p>
     *
     * @param submission Envío recibido (no se publica en esta implementación).
     */
    @Override
    public void publishSubmissionReceived(Submission submission) {
        log.debug("Mensajería desactivada: el envío {} no se publica en el broker.", submission.getId());
    }
}
