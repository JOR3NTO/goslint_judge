package co.uceva.auth.domain.exception;

/**
 * Excepción de dominio arrojada cuando los datos proporcionados para
 * operaciones de usuario no cumplen con las reglas de negocio (e.g. mal formato).
 */
public class InvalidUserDataException extends RuntimeException {
    /**
     * Constructor de la excepción.
     * @param message Mensaje detallado indicando qué dato es inválido.
     */
    public InvalidUserDataException(String message) {
        super(message);
    }
}
