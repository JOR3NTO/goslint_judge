package co.uceva.problem.domain.exception;

import java.util.UUID;

public class ExistingProblemException extends RuntimeException {

    public ExistingProblemException(UUID id){
        super("El problema con id '" + id + "' ya existe.");
    }

}
