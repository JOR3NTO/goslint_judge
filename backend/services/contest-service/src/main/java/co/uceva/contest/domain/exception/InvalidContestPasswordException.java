package co.uceva.contest.domain.exception;

import java.util.UUID;

/**
 * Excepción de dominio lanzada cuando la contraseña de inscripción suministrada
 * no coincide con la del concurso al que se intenta registrar un equipo.
 */
public class InvalidContestPasswordException extends RuntimeException {

    /**
     * Construye la excepción con el identificador del concurso.
     *
     * @param contestId Identificador del concurso.
     */
    public InvalidContestPasswordException(UUID contestId) {
        super("La contraseña del concurso '" + contestId + "' es incorrecta.");
    }
}
