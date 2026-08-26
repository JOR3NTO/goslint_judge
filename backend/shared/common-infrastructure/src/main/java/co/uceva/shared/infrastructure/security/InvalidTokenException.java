package co.uceva.shared.infrastructure.security;

/**
 * Excepción lanzada cuando un token no puede aceptarse como prueba de identidad.
 * <p>
 * Cubre por igual el token ausente, mal formado, expirado, firmado con otra clave
 * o sin los claims mínimos. El motivo concreto se registra pero no se devuelve al
 * cliente: distinguir "firma inválida" de "token expirado" solo ayudaría a quien
 * intenta adivinar un token válido.
 * </p>
 */
public class InvalidTokenException extends RuntimeException {

    /**
     * @param reason Descripción del motivo por el que el token fue rechazado.
     */
    public InvalidTokenException(String reason) {
        super("Token de autenticación inválido: " + reason);
    }

    /**
     * @param reason Descripción del motivo por el que el token fue rechazado.
     * @param cause  Excepción original ocurrida al analizar el token.
     */
    public InvalidTokenException(String reason, Throwable cause) {
        super("Token de autenticación inválido: " + reason, cause);
    }
}
