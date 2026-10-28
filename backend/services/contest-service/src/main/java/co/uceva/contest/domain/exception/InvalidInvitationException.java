package co.uceva.contest.domain.exception;

/**
 * Excepción de dominio lanzada cuando una invitación no puede procesarse: ya fue respondida,
 * se invalidó porque el equipo se llenó, o ya existe una pendiente para el mismo usuario.
 */
public class InvalidInvitationException extends RuntimeException {

    public InvalidInvitationException(String message) {
        super(message);
    }
}
