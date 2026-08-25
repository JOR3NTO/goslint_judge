package co.uceva.submission.application.port.out;

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
     *
     * @param submission El envío recién creado y persistido.
     */
    void publishSubmissionReceived(Submission submission);
}
