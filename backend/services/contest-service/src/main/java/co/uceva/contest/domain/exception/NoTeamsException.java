package co.uceva.contest.domain.exception;

/**
 * Excepción de dominio lanzada cuando se consulta una lista de equipos
 * pero no existe ninguno registrado para el criterio especificado.
 */
public class NoTeamsException extends RuntimeException {

    /**
     * Construye la excepción con un mensaje predeterminado.
     */
    public NoTeamsException() {
        super("No hay equipos.");
    }

    /**
     * Construye la excepción con un mensaje personalizado.
     *
     * @param message Mensaje descriptivo.
     */
    public NoTeamsException(String message) {
        super(message);
    }
}

