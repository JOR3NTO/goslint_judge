package co.uceva.auth.application.usecase.impl;

import co.uceva.auth.application.usecase.LogoutUserUseCase;
import co.uceva.auth.domain.repository.AuthCacheRepository;
import co.uceva.auth.domain.service.TokenProvider;
import org.springframework.stereotype.Service;

/**
 * Revoca la sesión del usuario de forma completa:
 * <ol>
 *   <li>Añade el access token a la lista negra con un TTL igual a su vigencia
 *       restante — la entrada desaparece sola cuando el token habría expirado
 *       de todos modos.</li>
 *   <li>Elimina el refresh token de Redis — aunque el access token expire,
 *       el cliente no puede obtener uno nuevo con un refresh token revocado.</li>
 * </ol>
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

        // 1. Blacklist del access token
        cacheRepository.blacklistToken(rawToken, ttlMillis);

        // 2. Revocación del refresh token — identificamos al usuario por el sub del JWT
        String userId = tokenProvider.getUserIdFromToken(rawToken);
        if (userId != null) {
            cacheRepository.deleteRefreshToken(userId);
        }
    }
}
