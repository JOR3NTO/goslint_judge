package co.uceva.problem.domain.exception;

import java.util.UUID;

public class TestCaseNotFoundException extends RuntimeException {

    public TestCaseNotFoundException(UUID testCaseId) {
        super("El caso de prueba con ID '" + testCaseId + "' no existe.");
    }

}