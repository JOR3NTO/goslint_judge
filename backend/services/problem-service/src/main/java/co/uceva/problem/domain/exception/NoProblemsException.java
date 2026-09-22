package co.uceva.problem.domain.exception;

/**
 * Excepción de dominio lanzada cuando se consulta una lista de problemas
 * pero no existe ninguno registrado.
 */
public class NoProblemsException extends RuntimeException {

    /** Construye la excepción con un mensaje predeterminado. */
    public NoProblemsException(){
        super("No hay problemas.");
    }

}
