package co.uceva.auth.application.usecase.impl;

import co.uceva.auth.application.usecase.LogoutUserUseCase;
import co.uceva.auth.domain.repository.AuthCacheRepository;
import co.uceva.auth.domain.service.TokenProvider;
import org.springframework.stereotype.Service;

/**
 * Revoca el access token del usuario añadiéndolo a la lista negra con un TTL
 * igual a su vigencia restante, de modo que la entrada desaparece sola cuando
 * el token habría expirado de todos modos.
 */
@Service
public class LogoutUserUseCaseImpl implements LogoutUserUseCase {

    private final AuthCacheRepository cacheRepository;
    private final TokenProvider tokenProvider;

    public LogoutUserUseCaseImpl(AuthCacheRepository cacheRepository, TokenProvider tokenProvider) {
        this.cacheRepository = cacheRepository;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        long ttlMillis = tokenProvider.getRemainingValidityMillis(rawToken);
        if (ttlMillis <= 0) {
            return; // El token ya expiró; no hay nada que revocar
        }

        cacheRepository.blacklistToken(rawToken, ttlMillis);
    }
}
