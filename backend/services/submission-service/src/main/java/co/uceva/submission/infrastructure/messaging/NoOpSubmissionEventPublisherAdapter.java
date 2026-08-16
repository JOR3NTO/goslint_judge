package co.uceva.submission.infrastructure.messaging;

import co.uceva.submission.application.port.out.SubmissionEventPublisher;
import co.uceva.submission.domain.model.Submission;
import org.springframework.stereotype.Component;

/**
 * Adaptador de infraestructura que implementa {@link SubmissionEventPublisher}
 * sin publicar realmente el evento.
 * <p>
 * Se utiliza como implementación temporal mientras se define la tecnología de
 * mensajería definitiva (RabbitMQ). De esta forma la capa de aplicación puede
 * depender del puerto de salida y compilar correctamente, y en el futuro solo
 * será necesario reemplazar este adaptador por uno real sin tocar el dominio
 * ni los casos de uso.
 * </p>
 */
@Component
public class NoOpSubmissionEventPublisherAdapter implements SubmissionEventPublisher {

    /**
     * No realiza ninguna acción. El evento de envío recibido se descarta
     * intencionalmente hasta que se conecte un broker de mensajería.
     *
     * @param submission Envío recibido (ignorado en esta implementación).
     */
    @Override
    public void publishSubmissionReceived(Submission submission) {
        // Implementación intencionalmente vacía.
        // TODO: reemplazar por adaptador RabbitMQ cuando se implemente la mensajería.
    }
}
