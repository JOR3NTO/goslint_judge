package co.uceva.judge.application.exception;

import java.util.UUID;

/**
 * Excepción de aplicación lanzada cuando el resultado de una evaluación no pudo
 * entregarse a {@code submission-service}. Se propaga para que la mensajería
 * reintente la evaluación completa.
 */
public class ResultPublishingException extends RuntimeException {

    /**
     * @param submissionId Identificador del envío cuyo resultado no se entregó.
     * @param reason       Motivo del fallo.
     */
    public ResultPublishingException(UUID submissionId, String reason) {
        super("No se pudo publicar el resultado del envío " + submissionId + ": " + reason);
    }

    /**
     * @param submissionId Identificador del envío cuyo resultado no se entregó.
     * @param reason       Motivo del fallo.
     * @param cause        Causa original.
     */
    public ResultPublishingException(UUID submissionId, String reason, Throwable cause) {
        super("No se pudo publicar el resultado del envío " + submissionId + ": " + reason, cause);
    }
}
