package co.uceva.auth.service;

import co.uceva.auth.cache.AuthCache;
import co.uceva.auth.exception.AccountDisabledException;
import co.uceva.auth.exception.BadCredentialsException;
import co.uceva.auth.exception.InvalidCurrentPasswordException;
import co.uceva.auth.exception.InvalidUserDataException;
import co.uceva.auth.exception.UsernameTakenException;
import co.uceva.auth.model.User;
import co.uceva.auth.repository.UserRepository;
import co.uceva.auth.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Perfil del usuario autenticado: consulta, cambio de nombre de usuario y de contraseña.
 * <p>
 * Cada operación recibe el access token crudo y lo valida con {@link #getCurrentUser}.
 * El formato de los datos nuevos lo valida el DTO de entrada.
 * </p>
 */
@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthCache authCache;
    private final JwtService jwtService;

    public ProfileService(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          AuthCache authCache,
                          JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authCache = authCache;
        this.jwtService = jwtService;
    }

    /**
     * Devuelve el usuario dueño del access token.
     *
     * @throws BadCredentialsException si el token falta, es inválido, expiró, es un refresh
     *                                 token, fue revocado o su usuario ya no existe.
     * @throws AccountDisabledException si la cuenta fue desactivada.
     */
    public User getCurrentUser(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadCredentialsException("Token no proporcionado");
        }
        if (authCache.isBlacklisted(rawToken)) {
            throw new BadCredentialsException("Sesión cerrada o token revocado");
        }

        UUID userId = jwtService.parseAccessToken(rawToken)
                .orElseThrow(() -> new BadCredentialsException("Token inválido o expirado"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado"));
        if (!user.isActive()) {
            throw new AccountDisabledException("La cuenta de usuario se encuentra desactivada");
        }
        return user;
    }

    /**
     * Cambia el nombre de usuario. Es único sin distinguir mayúsculas, pero el dueño sí
     * puede cambiar solo la capitalización del suyo.
     *
     * @throws UsernameTakenException si otra cuenta ya lo usa.
     */
    public User updateUsername(String rawToken, String newUsername) {
        User user = getCurrentUser(rawToken);
        String username = newUsername.trim();

        if (user.getUsername().equals(username)) {
            return user;
        }
        if (userRepository.existsByUsernameIgnoreCaseAndIdNot(username, user.getId())) {
            throw new UsernameTakenException(
                    "El nombre de usuario '" + username + "' ya está en uso por otra cuenta.");
        }

        user.setUsername(username);
        return userRepository.save(user);
    }

    /**
     * Cambia la contraseña y cierra todas las sesiones: revoca el access token usado y
     * borra el refresh token, de modo que el usuario debe volver a iniciar sesión.
     *
     * @throws InvalidCurrentPasswordException si la contraseña actual no coincide.
     * @throws InvalidUserDataException si la nueva es igual a la actual.
     */
    public void changePassword(String rawToken, String currentPassword, String newPassword) {
        User user = getCurrentUser(rawToken);

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new InvalidCurrentPasswordException("La contraseña actual es incorrecta.");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new InvalidUserDataException("La nueva contraseña debe ser diferente a la contraseña actual.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        jwtService.parse(rawToken)
                .filter(info -> info.remainingMillis() > 0)
                .ifPresent(info -> authCache.blacklistToken(rawToken, info.remainingMillis()));
        authCache.deleteRefreshToken(user.getId().toString());
    }
}
