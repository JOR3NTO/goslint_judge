package co.uceva.contest.domain.exception;

import java.util.UUID;

/**
 * Excepción de dominio lanzada cuando se intenta acceder a un concurso
 * que no existe en el sistema.
 */
public class ContestNotFoundException extends RuntimeException {

    /**
     * Construye la excepción con el identificador del concurso no encontrado.
     *
     * @param contestId Identificador del concurso buscado.
     */
    public ContestNotFoundException(UUID contestId) {
        super("El concurso con ID '" + contestId + "' no existe.");
    }
}

