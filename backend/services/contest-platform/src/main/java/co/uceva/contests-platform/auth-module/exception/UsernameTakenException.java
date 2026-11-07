package co.uceva.contestsplatform.auth.exception;

/**
 * El nuevo nombre de usuario ya pertenece a otra cuenta.
 */
public class UsernameTakenException extends RuntimeException {
    public UsernameTakenException(String message) {
        super(message);
    }
}
