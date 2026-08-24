package co.uceva.problem.domain.exception;

import java.util.UUID;

/**
 * Excepción de dominio lanzada cuando se intenta acceder a un caso de prueba
 * que no existe en el sistema.
 */
public class TestCaseNotFoundException extends RuntimeException {

    /**
     * Construye la excepción con el identificador del caso de prueba no encontrado.
     *
     * @param testCaseId Identificador del caso de prueba buscado.
     */
    public TestCaseNotFoundException(UUID testCaseId) {
        super("El caso de prueba con ID '" + testCaseId + "' no existe.");
    }

}