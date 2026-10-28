package co.uceva.auth.service;

import co.uceva.auth.cache.AuthCache;
import co.uceva.auth.dto.LoginResponse;
import co.uceva.auth.dto.RegisterRequest;
import co.uceva.auth.exception.AccountDisabledException;
import co.uceva.auth.exception.AccountLockedException;
import co.uceva.auth.exception.BadCredentialsException;
import co.uceva.auth.exception.UserAlreadyExistsException;
import co.uceva.auth.model.User;
import co.uceva.auth.repository.UserRepository;
import co.uceva.auth.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * Registro, inicio y cierre de sesión, y renovación de tokens.
 */
@Service
public class AuthService {

    static final int MAX_FAILED_ATTEMPTS = 5;
    static final long LOCK_MINUTES = 15;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthCache authCache;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthCache authCache,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authCache = authCache;
        this.jwtService = jwtService;
    }

    /**
     * Registra un estudiante nuevo. El correo se guarda en minúsculas y tanto él como
     * el nombre de usuario son únicos sin distinguir mayúsculas.
     */
    public User register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        String username = request.username().trim();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new UserAlreadyExistsException("El correo electrónico ya está registrado.");
        }
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new UserAlreadyExistsException("El nombre de usuario ya está registrado.");
        }

        User user = User.newStudent(
                request.firstName().trim(),
                request.lastName().trim(),
                username,
                email,
                passwordEncoder.encode(request.password()),
                request.institution());
        return userRepository.save(user);
    }

    /**
     * Valida las credenciales y emite un par de tokens. Cinco fallos seguidos bloquean
     * el correo durante 15 minutos. Usuario inexistente, contraseña errónea y cuenta
     * inactiva responden igual para no revelar qué correos están registrados.
     */
    public LoginResponse login(String rawEmail, String password) {
        String email = normalizeEmail(rawEmail);

        if (authCache.isAccountLocked(email)) {
            long minutes = (authCache.getLockSecondsLeft(email) + 59) / 60;
            throw new AccountLockedException(
                    "Demasiados intentos fallidos. Por favor, espere " + Math.max(1, minutes) + " minutos.");
        }

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        boolean valid = user != null
                && passwordEncoder.matches(password, user.getPasswordHash())
                && user.isActive();

        if (!valid) {
            if (authCache.registerFailedAttempt(email) >= MAX_FAILED_ATTEMPTS) {
                authCache.lockAccount(email, LOCK_MINUTES);
                authCache.resetFailedAttempts(email);
                throw new AccountLockedException(
                        "Su cuenta ha sido bloqueada por " + LOCK_MINUTES
                                + " minutos debido a múltiples intentos fallidos.");
            }
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        authCache.resetFailedAttempts(email);
        return issueTokens(user);
    }

    /**
     * Cierra la sesión: revoca el token recibido hasta que expire y borra el refresh
     * token del usuario. Si el token es inválido o ya expiró no hay nada que revocar.
     */
    public void logout(String rawToken) {
        jwtService.parse(rawToken)
                .filter(info -> info.remainingMillis() > 0)
                .ifPresent(info -> {
                    authCache.blacklistToken(rawToken, info.remainingMillis());
                    authCache.deleteRefreshToken(info.userId().toString());
                });
    }

    /**
     * Canjea un refresh token vigente por un par nuevo (rotación). El refresh token
     * usado deja de servir porque el nuevo lo reemplaza en Redis.
     */
    public LoginResponse refresh(String refreshToken) {
        UUID userId = jwtService.parseRefreshToken(refreshToken)
                .orElseThrow(() -> new BadCredentialsException("Refresh token inválido o expirado"));

        String stored = authCache.getRefreshToken(userId.toString());
        if (stored == null || !constantTimeEquals(stored, refreshToken)) {
            throw new BadCredentialsException("Refresh token inválido o expirado");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado"));
        if (!user.isActive()) {
            throw new AccountDisabledException("La cuenta de usuario se encuentra desactivada");
        }

        return issueTokens(user);
    }

    private LoginResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        authCache.saveRefreshToken(user.getId().toString(), refreshToken, jwtService.getRefreshExpirationMs());
        return LoginResponse.bearer(accessToken, refreshToken, jwtService.getAccessExpirationMs());
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
