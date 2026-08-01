package co.uceva.problem.domain.exception;

import java.util.UUID;

public class ExistingTestCaseException extends RuntimeException {

    public ExistingTestCaseException(UUID id){
        super("El caso de prueba con id '" + id + "' ya existe.");
    }

}
