package co.uceva.contest.domain.exception;

import java.util.UUID;

/**
 * Excepción de dominio lanzada cuando se intenta inscribir o invitar a un equipo
 * en un concurso que ya inició.
 */
public class RegistrationClosedException extends RuntimeException {

    public RegistrationClosedException(UUID contestId) {
        super("Las inscripciones e invitaciones del concurso '" + contestId + "' están cerradas porque ya inició.");
    }
}
