package co.uceva.submission.application.exception;

import java.util.UUID;

/**
 * Excepción lanzada cuando la entrega de un evento al sistema de mensajería no
 * pudo confirmarse.
 * <p>
 * Señala exclusivamente que la entrega <em>no está garantizada</em>: el broker
 * puede estar caído, haber rechazado el mensaje o no haber respondido a tiempo.
 * Nunca implica que el envío se haya perdido, ya que en ese momento el envío
 * está persistido en estado {@code PENDING} y será reintentado.
 * </p>
 */
public class EventPublishingException extends RuntimeException {

    /**
     * @param submissionId Identificador del envío cuya publicación falló.
     * @param reason       Descripción del motivo por el que no se confirmó la entrega.
     */
    public EventPublishingException(UUID submissionId, String reason) {
        super("No se pudo confirmar la entrega del envío %s al motor de evaluación: %s"
                .formatted(submissionId, reason));
    }

    /**
     * @param submissionId Identificador del envío cuya publicación falló.
     * @param reason       Descripción del motivo por el que no se confirmó la entrega.
     * @param cause        Excepción original ocurrida en la capa de mensajería.
     */
    public EventPublishingException(UUID submissionId, String reason, Throwable cause) {
        super("No se pudo confirmar la entrega del envío %s al motor de evaluación: %s"
                .formatted(submissionId, reason), cause);
    }
}
