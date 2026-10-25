package co.uceva.contest.domain.exception;

import java.util.UUID;

/**
 * Excepción de dominio lanzada cuando se intenta asignar a un concurso un problema
 * o un alias que ya están en uso dentro del mismo concurso.
 */
public class ExistingContestProblemException extends RuntimeException {

    /**
     * Construye la excepción indicando el problema y el concurso donde ya está asignado.
     *
     * @param problemId Identificador del problema duplicado.
     * @param contestId Identificador del concurso.
     */
    public ExistingContestProblemException(UUID problemId, UUID contestId) {
        super("El problema '" + problemId + "' ya está asignado al concurso '" + contestId + "'.");
    }

    /**
     * Construye la excepción con un mensaje personalizado (p. ej. alias repetido).
     *
     * @param message Mensaje descriptivo del error.
     */
    public ExistingContestProblemException(String message) {
        super(message);
    }
}
