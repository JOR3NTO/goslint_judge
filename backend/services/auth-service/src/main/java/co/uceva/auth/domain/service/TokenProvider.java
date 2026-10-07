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
}
