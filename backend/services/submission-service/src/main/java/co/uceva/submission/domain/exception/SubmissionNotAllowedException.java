package co.uceva.submission.domain.exception;

import co.uceva.submission.domain.model.SubmissionRejectionReason;

/**
 * Excepción lanzada cuando un envío incumple la regla de elegibilidad: solo se
 * acepta si el usuario es concursante de un equipo, ese equipo está en un concurso
 * activo y el problema pertenece a ese concurso.
 * <p>
 * El mensaje depende del motivo para que el cliente pueda explicar al usuario por
 * qué se rechazó su envío sin tener que interpretar códigos internos.
 * </p>
 */
public class SubmissionNotAllowedException extends RuntimeException {

    private final SubmissionRejectionReason reason;

    /**
     * @param reason Motivo concreto del rechazo.
     */
    public SubmissionNotAllowedException(SubmissionRejectionReason reason) {
        super(messageFor(reason));
        this.reason = reason;
    }

    /** @return El motivo concreto del rechazo. */
    public SubmissionRejectionReason getReason() {
        return reason;
    }

    private static String messageFor(SubmissionRejectionReason reason) {
        return switch (reason) {
            case TEAM_NOT_FOUND -> "El equipo indicado no existe.";
            case NOT_A_MEMBER -> "El usuario no es integrante del equipo indicado.";
            case CONTEST_NOT_FOUND -> "El concurso no existe.";
            case CONTEST_NOT_ACTIVE -> "El concurso no está activo en este momento.";
            case PROBLEM_NOT_IN_CONTEST -> "El problema no pertenece al concurso.";
        };
    }
}
