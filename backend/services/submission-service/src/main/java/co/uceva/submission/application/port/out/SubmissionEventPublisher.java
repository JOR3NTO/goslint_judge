package co.uceva.submission.application.port.out;

import co.uceva.submission.application.exception.EventPublishingException;
import co.uceva.submission.domain.model.Submission;

/**
 * Puerto de salida para publicar eventos relacionados con envíos de código.
 * <p>
 * Define el contrato que la capa de infraestructura debe implementar para
 * notificar a otros servicios (ej. judge-service) sobre nuevos envíos,
 * típicamente a través de un sistema de mensajería como RabbitMQ.
 * </p>
 */
public interface SubmissionEventPublisher {

    /**
     * Publica un evento indicando que se ha recibido un nuevo envío y
     * está listo para ser evaluado por el motor de juzgamiento.
     * <p>
     * El contrato es <em>publicar y confirmar</em>, no publicar y olvidar: la
     * llamada solo retorna con normalidad cuando la entrega ha sido confirmada
     * por el sistema de mensajería. Esto es lo que permite al llamador afirmar
     * con certeza que el envío quedó encolado.
     * </p>
     *
     * @param submission El envío recién creado y persistido.
     * @throws EventPublishingException Si la entrega no pudo confirmarse.
     */
    void publishSubmissionReceived(Submission submission);
}
