package co.uceva.judge.application.exception;

import java.util.UUID;

/**
 * Excepción de aplicación lanzada cuando el aviso de inicio de evaluación no
 * pudo entregarse a {@code submission-service}. Se propaga para que la
 * mensajería reintente la evaluación completa.
 */
public class JudgingNotificationException extends RuntimeException {

    /**
     * @param submissionId Identificador del envío cuyo aviso no se entregó.
     * @param reason       Motivo del fallo.
     */
    public JudgingNotificationException(UUID submissionId, String reason) {
        super("No se pudo notificar el inicio de la evaluación del envío " + submissionId + ": " + reason);
    }

    /**
     * @param submissionId Identificador del envío cuyo aviso no se entregó.
     * @param reason       Motivo del fallo.
     * @param cause        Causa original.
     */
    public JudgingNotificationException(UUID submissionId, String reason, Throwable cause) {
        super("No se pudo notificar el inicio de la evaluación del envío " + submissionId + ": " + reason, cause);
    }
}
