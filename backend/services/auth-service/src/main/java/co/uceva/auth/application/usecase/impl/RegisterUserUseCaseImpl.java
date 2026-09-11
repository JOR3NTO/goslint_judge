package co.uceva.auth.application.service;

import co.uceva.auth.application.port.in.RegisterUserUseCase;
import co.uceva.auth.application.port.out.PasswordEncoderPort;
import co.uceva.auth.application.port.out.UserRepository;
import co.uceva.auth.domain.exception.UserAlreadyExistsException;
import co.uceva.auth.domain.model.User;
import org.springframework.stereotype.Service;

/**
 * Servicio de Aplicación (Caso de Uso) que implementa la lógica principal
 * de registro de usuarios. Es el Orquestador central de la capa de aplicación.
 */
@Service
public class AuthApplicationService implements RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;

    /**
     * Inyección de dependencias mediante constructor.
     * Recibe los puertos de salida (que serán implementados en la capa de infraestructura).
     */
    public AuthApplicationService(UserRepository userRepository, PasswordEncoderPort passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
}
