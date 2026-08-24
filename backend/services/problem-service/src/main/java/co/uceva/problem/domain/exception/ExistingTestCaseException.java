package co.uceva.problem.domain.exception;

import java.util.UUID;

/**
 * Excepción de dominio lanzada cuando se intenta crear un caso de prueba
 * que ya existe en el sistema.
 */
public class ExistingTestCaseException extends RuntimeException {

    /**
     * Construye la excepción con el identificador del caso de prueba duplicado.
     *
     * @param id Identificador del caso de prueba existente.
     */
    public ExistingTestCaseException(UUID id){
        super("El caso de prueba con id '" + id + "' ya existe.");
    }

}
