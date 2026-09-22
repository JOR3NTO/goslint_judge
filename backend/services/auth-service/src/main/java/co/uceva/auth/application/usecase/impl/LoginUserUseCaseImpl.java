package co.uceva.auth.application.usecase.impl;

import co.uceva.auth.domain.service.PasswordEncoder;
import co.uceva.auth.domain.repository.UserRepository;
import co.uceva.auth.application.usecase.LoginUserUseCase;
import co.uceva.auth.domain.exception.AccountLockedException;
import co.uceva.auth.domain.exception.BadCredentialsException;
import co.uceva.auth.domain.model.AuthToken;
import co.uceva.auth.domain.model.User;
import co.uceva.auth.domain.repository.AuthCacheRepository;
import co.uceva.auth.domain.service.TokenProvider;
import org.springframework.stereotype.Service;

/**
 * Implementación del caso de uso de inicio de sesión.
 * Orquesta la verificación de credenciales, el control de intentos fallidos
 * y la generación de tokens JWT (access + refresh).
 */
@Service
public class LoginUserUseCaseImpl implements LoginUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordEncoder passwordEncoder;
    private final AuthCacheRepository authCacheRepository;
    private final TokenProvider tokenProvider;

    public LoginUserUseCaseImpl(UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                PasswordEncoder passwordEncoder,
                                AuthCacheRepository authCacheRepository,
                                TokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authCacheRepository = authCacheRepository;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public AuthToken login(String email, String password) {
        if (authCacheRepository.isAccountLocked(email)) {
            long waitTime = authCacheRepository.getLockTimeLeftSeconds(email) / 60;
            throw new AccountLockedException("Demasiados intentos fallidos. Por favor, espere " + waitTime + " minutos.");
        }

        User user = userRepository.findByEmail(email).orElse(null);

        boolean passwordMatches = false;
        if (user != null) {
            passwordMatches = passwordEncoder.matches(password, user.getPasswordHash());
        }

        // Si el usuario no existe, la contraseña no coincide o la cuenta no está activa,
        // lanzamos siempre el mismo error para evitar la enumeración de usuarios (seguridad).
        if (user == null || !passwordMatches || !user.isActive()) {
            authCacheRepository.incrementFailedAttempts(email);
            int attempts = authCacheRepository.getFailedAttempts(email);
            if (attempts >= 5) {
                authCacheRepository.lockAccount(email, 15);
                throw new AccountLockedException("Su cuenta ha sido bloqueada por 15 minutos debido a múltiples intentos fallidos.");
            }
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        authCacheRepository.resetFailedAttempts(email);

        String accessToken = tokenProvider.generateAccessToken(user);
        String refreshToken = tokenProvider.generateRefreshToken(user);

        // Guardamos el refresh token por 8 horas (28800000 ms)
        authCacheRepository.saveRefreshToken(user.getId().toString(), refreshToken, 28800000);

        return AuthToken.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(tokenProvider.getAccessTokenExpirationMs())
                .build();
    }
}
