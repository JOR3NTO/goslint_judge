package co.uceva.contest.domain.exception;

import java.util.UUID;

/**
 * Excepción de dominio lanzada cuando se busca un problema que no está
 * asignado al concurso indicado.
 */
public class ContestProblemNotFoundException extends RuntimeException {

    /**
     * Construye la excepción con el identificador del problema de concurso no encontrado.
     *
     * @param contestProblemId Identificador del problema de concurso buscado.
     */
    public ContestProblemNotFoundException(UUID contestProblemId) {
        super("El problema de concurso con ID '" + contestProblemId + "' no existe.");
    }

    /**
     * Construye la excepción con un mensaje personalizado.
     *
     * @param message Mensaje descriptivo del error.
     */
    public ContestProblemNotFoundException(String message) {
        super(message);
    }
}
