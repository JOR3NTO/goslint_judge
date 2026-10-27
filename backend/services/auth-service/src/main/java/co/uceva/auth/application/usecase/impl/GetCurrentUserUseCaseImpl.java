package co.uceva.auth.application.usecase.impl;

import co.uceva.auth.application.usecase.GetCurrentUserUseCase;
import co.uceva.auth.domain.exception.AccountDisabledException;
import co.uceva.auth.domain.exception.BadCredentialsException;
import co.uceva.auth.domain.model.User;
import co.uceva.auth.domain.repository.AuthCacheRepository;
import co.uceva.auth.domain.repository.UserRepository;
import co.uceva.auth.domain.service.TokenProvider;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetCurrentUserUseCaseImpl implements GetCurrentUserUseCase {

    private final TokenProvider tokenProvider;
    private final AuthCacheRepository authCacheRepository;
    private final UserRepository userRepository;

    public GetCurrentUserUseCaseImpl(TokenProvider tokenProvider,
                                     AuthCacheRepository authCacheRepository,
                                     UserRepository userRepository) {
        this.tokenProvider = tokenProvider;
        this.authCacheRepository = authCacheRepository;
        this.userRepository = userRepository;
    }

    @Override
    public User getCurrentUser(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadCredentialsException("Token no proporcionado");
        }

        // 1. Verificar si el token fue invalidado en Redis (logout)
        if (authCacheRepository.isTokenBlacklisted(rawToken)) {
            throw new BadCredentialsException("Sesión cerrada o token revocado");
        }

        // 2. Extraer el subject (userId) del JWT
        String userIdStr = tokenProvider.getUserIdFromToken(rawToken);
        if (userIdStr == null) {
            throw new BadCredentialsException("Token inválido o expirado");
        }

        UUID userId;
        try {
            userId = UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("Formato de identificador de usuario inválido");
        }

        // 3. Buscar al usuario en PostgreSQL
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado"));

        // 4. Verificar que la cuenta esté activa
        if (!user.isActive()) {
            throw new AccountDisabledException("La cuenta de usuario se encuentra desactivada");
        }

        return user;
    }
}
