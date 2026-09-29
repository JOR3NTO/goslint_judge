package co.uceva.judge.domain.exception;

import java.util.UUID;

/**
 * Excepción de dominio lanzada cuando un problema no tiene casos de prueba
 * contra los cuales evaluar un envío.
 */
public class TestCasesNotFoundException extends RuntimeException {

    /**
     * Construye la excepción con el identificador del problema sin casos de prueba.
     *
     * @param problemId Identificador del problema consultado.
     */
    public TestCasesNotFoundException(UUID problemId) {
        super("El problema con ID '" + problemId + "' no tiene casos de prueba.");
    }
}
