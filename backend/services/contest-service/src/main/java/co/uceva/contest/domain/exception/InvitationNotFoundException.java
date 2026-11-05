package co.uceva.contest.domain.exception;

import java.util.UUID;

/**
 * Excepción de dominio lanzada cuando una invitación no existe.
 */
public class InvitationNotFoundException extends RuntimeException {

    public InvitationNotFoundException(UUID invitationId) {
        super("La invitación con ID '" + invitationId + "' no existe.");
    }
}
