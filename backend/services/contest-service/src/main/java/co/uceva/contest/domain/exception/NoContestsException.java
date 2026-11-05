package co.uceva.contest.domain.exception;

/**
 * Excepción de dominio lanzada cuando se consulta una lista de concursos
 * pero no existe ninguno registrado.
 */
public class NoContestsException extends RuntimeException {

    /**
     * Construye la excepción con un mensaje predeterminado.
     */
    public NoContestsException() {
        super("No hay concursos.");
    }
}

