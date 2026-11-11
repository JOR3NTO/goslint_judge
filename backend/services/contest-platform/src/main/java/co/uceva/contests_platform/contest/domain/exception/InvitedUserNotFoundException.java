package co.uceva.contests_platform.contest.domain.exception;

import java.util.UUID;

/** Excepción de dominio: el usuario al que se quiere invitar no existe. */
public class InvitedUserNotFoundException extends RuntimeException {

    public InvitedUserNotFoundException(UUID userId) {
        super("No existe el usuario invitado con id " + userId + ".");
    }
}
