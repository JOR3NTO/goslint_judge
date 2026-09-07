package co.uceva.auth.application.port.out;

import co.uceva.auth.domain.model.User;

public interface TokenProviderPort {
    String generateAccessToken(User user);
    String generateRefreshToken(User user);
    Long getAccessTokenExpirationMs();
}
