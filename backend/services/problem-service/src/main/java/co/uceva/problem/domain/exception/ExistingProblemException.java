package co.uceva.problem.domain.exception;

import java.util.UUID;

/**
 * Excepción de dominio lanzada cuando se intenta crear un problema
 * que ya existe en el sistema.
 */
public class ExistingProblemException extends RuntimeException {

    /**
     * Construye la excepción con el identificador del problema duplicado.
     *
     * @param id Identificador del problema existente.
     */
    public ExistingProblemException(UUID id){
        super("El problema con id '" + id + "' ya existe.");
    }

}
