package co.uceva.shared.infrastructure.security;

/**
 * Puerto de consulta para saber si un token ha sido revocado explícitamente.
 * <p>
 * Permite que {@link JwtTokenValidator} rechace tokens que, aunque tengan firma
 * y vigencia válidas, hayan sido invalidados por una operación de cierre de
 * sesión. La implementación concreta consulta Redis; la interfaz vive aquí para
 * que la librería compartida no dependa de ningún servicio concreto.
 * </p>
 */
public interface TokenBlacklist {
    boolean isBlacklisted(String rawToken);
}

