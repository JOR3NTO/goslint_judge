package co.uceva.auth.exception;

/**
 * Datos de usuario que incumplen una regla de negocio.
 */
public class InvalidUserDataException extends RuntimeException {
    public InvalidUserDataException(String message) {
        super(message);
    }
}
