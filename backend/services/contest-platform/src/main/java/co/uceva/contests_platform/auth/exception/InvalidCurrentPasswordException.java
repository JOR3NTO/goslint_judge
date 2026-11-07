package co.uceva.contests_platform.auth.exception;

/**
 * La contraseña actual indicada al cambiar de contraseña no coincide.
 */
public class InvalidCurrentPasswordException extends RuntimeException {
    public InvalidCurrentPasswordException(String message) {
        super(message);
    }
}
