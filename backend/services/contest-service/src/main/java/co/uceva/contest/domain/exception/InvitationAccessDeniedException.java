package co.uceva.contest.domain.exception;

/**
 * Excepción de dominio lanzada cuando un usuario intenta una acción de invitaciones
 * que no le corresponde (invitar sin ser capitán, responder una invitación ajena).
 */
public class InvitationAccessDeniedException extends RuntimeException {

    public InvitationAccessDeniedException(String message) {
        super(message);
    }
}
