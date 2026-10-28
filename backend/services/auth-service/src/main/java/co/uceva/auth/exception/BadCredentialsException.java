package co.uceva.auth.exception;

/**
 * Credenciales incorrectas, o token ausente, inválido, expirado o revocado.
 */
public class BadCredentialsException extends RuntimeException {
    public BadCredentialsException(String message) {
        super(message);
    }
}
