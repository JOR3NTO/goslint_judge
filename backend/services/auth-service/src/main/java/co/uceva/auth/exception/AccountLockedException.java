package co.uceva.auth.exception;

/**
 * La cuenta está bloqueada temporalmente por demasiados intentos fallidos.
 */
public class AccountLockedException extends RuntimeException {
    public AccountLockedException(String message) {
        super(message);
    }
}
