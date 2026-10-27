package co.uceva.auth.domain.service;

import co.uceva.auth.domain.model.User;

public interface TokenProvider {
    String generateAccessToken(User user);
    String generateRefreshToken(User user);
    Long getAccessTokenExpirationMs();

    /**
     * @param rawToken Token JWT compacto (sin prefijo Bearer).
     * @return Milisegundos restantes de vigencia del token, o 0 si ya expiró o no es válido.
     */
    long getRemainingValidityMillis(String rawToken);

    /**
     * Extrae el identificador de usuario ({@code sub}) del token.
     *
     * @param rawToken Token JWT compacto (sin prefijo Bearer).
     * @return El UUID del usuario como String, o {@code null} si el token no es válido.
     */
    String getUserIdFromToken(String rawToken);
}
