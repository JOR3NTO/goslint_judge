package co.uceva.problem.domain.exception;

import java.util.UUID;

public class ProblemNotFoundException extends RuntimeException {

    public ProblemNotFoundException(UUID problemId) {
        super("El problema con ID '" + problemId + "' no existe.");
    }

}
