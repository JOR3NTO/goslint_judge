package co.uceva.problem.domain.exception;

public class NoProblemsException extends RuntimeException {

    public NoProblemsException(){
        super("No hay problemas.");
    }

}
