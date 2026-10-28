package co.uceva.auth.domain.service;

import co.uceva.auth.domain.model.User;

public interface TokenProvider {
    String generateAccessToken(User user);
    String generateRefreshToken(User user);
    Long getAccessTokenExpirationMs();
}
