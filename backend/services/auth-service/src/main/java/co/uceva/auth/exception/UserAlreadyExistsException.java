package co.uceva.auth.exception;

/**
 * El correo o el nombre de usuario ya están registrados.
 */
public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
