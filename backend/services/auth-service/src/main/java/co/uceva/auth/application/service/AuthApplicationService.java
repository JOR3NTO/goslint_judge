package co.uceva.auth.application.service;

import co.uceva.auth.application.port.in.RegisterUserUseCase;
import co.uceva.auth.application.port.out.PasswordEncoderPort;
import co.uceva.auth.application.port.out.UserRepository;
import co.uceva.auth.domain.exception.UserAlreadyExistsException;
import co.uceva.auth.domain.model.User;
import org.springframework.stereotype.Service;

import co.uceva.auth.application.port.in.LoginUserUseCase;
import co.uceva.auth.application.port.out.CachePort;
import co.uceva.auth.application.port.out.TokenProviderPort;
import co.uceva.auth.domain.exception.AccountDisabledException;
import co.uceva.auth.domain.exception.AccountLockedException;
import co.uceva.auth.domain.exception.BadCredentialsException;
import co.uceva.auth.domain.model.AuthToken;

/**
 * Servicio de Aplicación (Caso de Uso) que implementa la lógica principal
 * de registro de usuarios y autenticación. Es el Orquestador central de la capa de aplicación.
 */
@Service
public class AuthApplicationService implements RegisterUserUseCase, LoginUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final CachePort cachePort;
    private final TokenProviderPort tokenProviderPort;

    /**
     * Inyección de dependencias mediante constructor.
     * Recibe los puertos de salida (que serán implementados en la capa de infraestructura).
     */
    public AuthApplicationService(UserRepository userRepository, 
                                  PasswordEncoderPort passwordEncoder,
                                  CachePort cachePort,
                                  TokenProviderPort tokenProviderPort) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.cachePort = cachePort;
        this.tokenProviderPort = tokenProviderPort;
    }

    /**
     * Ejecuta el flujo principal de negocio para registrar un usuario:
     * 1. Valida que el email no exista.
     * 2. Valida que el username no exista.
     * 3. Hashea la contraseña.
     * 4. Construye el objeto de dominio y lo guarda.
     */
    @Override
    public User register(String username, String email, String password, String institution) {
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("El correo electrónico ya está registrado.");
        }
        
        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException("El nombre de usuario ya está registrado.");
        }

        // Se encripta la contraseña usando el puerto (no acoplado a BCrypt directo aquí)
        String hashedPassword = passwordEncoder.encode(password);
        
        // Uso del factory method del dominio
        User newUser = User.createNewStudent(username, email, hashedPassword, institution);
        
        // Se guarda utilizando el puerto del repositorio
        return userRepository.save(newUser);
    }

    @Override
    public AuthToken login(String email, String password) {
        if (cachePort.isAccountLocked(email)) {
            long waitTime = cachePort.getLockTimeLeftSeconds(email) / 60;
            throw new AccountLockedException("Demasiados intentos fallidos. Por favor, espere " + waitTime + " minutos.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas"));

        if (!user.isActive()) {
            throw new AccountDisabledException("Su cuenta no está activa. Verifique su correo electrónico o contacte a soporte.");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            cachePort.incrementFailedAttempts(email);
            int attempts = cachePort.getFailedAttempts(email);
            if (attempts >= 5) {
                cachePort.lockAccount(email, 15);
                throw new AccountLockedException("Su cuenta ha sido bloqueada por 15 minutos debido a múltiples intentos fallidos.");
            }
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        cachePort.resetFailedAttempts(email);

        String accessToken = tokenProviderPort.generateAccessToken(user);
        String refreshToken = tokenProviderPort.generateRefreshToken(user);
        
        // Guardamos el refresh token por 8 horas (28800000 ms)
        cachePort.saveRefreshToken(user.getId().toString(), refreshToken, 28800000);

        return AuthToken.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(tokenProviderPort.getAccessTokenExpirationMs())
                .build();
    }
}
