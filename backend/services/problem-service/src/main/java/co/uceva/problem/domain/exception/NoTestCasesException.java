package co.uceva.problem.domain.exception;

/**
 * Excepción de dominio lanzada cuando se consulta una lista de casos de prueba
 * pero no existe ninguno registrado.
 */
public class NoTestCasesException extends RuntimeException {

    /** Construye la excepción con un mensaje predeterminado. */
    public NoTestCasesException(){
        super("No hay casos de prueba.");
    }

}
