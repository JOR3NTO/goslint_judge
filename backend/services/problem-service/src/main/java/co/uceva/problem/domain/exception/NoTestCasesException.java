package co.uceva.problem.domain.exception;

public class NoTestCasesException extends RuntimeException {

    public NoTestCasesException(){
        super("No hay casos de prueba.");
    }

}
