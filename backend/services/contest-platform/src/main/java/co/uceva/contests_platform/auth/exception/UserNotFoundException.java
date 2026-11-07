package co.uceva.contests_platform.auth.exception;

/**
 * No existe un usuario con el identificador indicado.
 */
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
