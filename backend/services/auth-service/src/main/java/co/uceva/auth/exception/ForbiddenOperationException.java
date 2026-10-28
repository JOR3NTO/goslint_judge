package co.uceva.auth.exception;

/**
 * El usuario autenticado no tiene permiso para esta operación: no es administrador,
 * o la regla de jerarquía o de autoprotección la impide.
 */
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
