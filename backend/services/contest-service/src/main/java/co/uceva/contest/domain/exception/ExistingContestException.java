package co.uceva.contest.domain.exception;

import java.util.UUID;

/**
 * Excepción de dominio lanzada cuando se intenta crear un concurso
 * que ya existe en el sistema.
 */
public class ExistingContestException extends RuntimeException {

    /**
     * Construye la excepción con el identificador del concurso duplicado.
     *
     * @param id Identificador del concurso existente.
     */
    public ExistingContestException(UUID id) {
        super("El concurso con id '" + id + "' ya existe.");
    }
}

