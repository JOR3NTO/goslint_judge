package co.uceva.auth.exception;

/**
 * La cuenta existe pero fue desactivada por un administrador.
 */
public class AccountDisabledException extends RuntimeException {
    public AccountDisabledException(String message) {
        super(message);
    }
}
