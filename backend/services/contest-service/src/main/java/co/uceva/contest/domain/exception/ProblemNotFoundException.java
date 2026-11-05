package co.uceva.contest.domain.exception;

import java.util.UUID;

/**
 * Excepción de dominio lanzada cuando se referencia un problema que no existe en el catálogo.
 */
public class ProblemNotFoundException extends RuntimeException {

    /**
     * Construye la excepción con el identificador del problema no encontrado.
     *
     * @param problemId Identificador del problema buscado.
     */
    public ProblemNotFoundException(UUID problemId) {
        super("El problema con ID '" + problemId + "' no existe.");
    }
}
