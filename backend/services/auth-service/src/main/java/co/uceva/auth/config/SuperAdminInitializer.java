package co.uceva.auth.config;

import co.uceva.auth.dto.ValidationRules;
import co.uceva.auth.model.Role;
import co.uceva.auth.model.User;
import co.uceva.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Crea el super administrador al arrancar, una sola vez, a partir de variables de entorno.
 * Es la única vía para obtener ese rol: ni el registro ni la API de administración lo asignan.
 * <p>
 * Si ya existe algún {@code SUPER_ADMIN} no hace nada (nunca cambia credenciales existentes). Si
 * no hay variables configuradas tampoco hace nada y solo lo registra en el log.
 * </p>
 */
@Component
public class SuperAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String username;
    private final String password;

    public SuperAdminInitializer(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 @Value("${app.super-admin.email:}") String email,
                                 @Value("${app.super-admin.username:superadmin}") String username,
                                 @Value("${app.super-admin.password:}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email.trim().toLowerCase();
        this.username = username.trim();
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(Role.SUPER_ADMIN)) {
            return;
        }
        if (email.isEmpty() || password.isEmpty()) {
            log.warn("No existe ningún super administrador. Defina SUPER_ADMIN_EMAIL y SUPER_ADMIN_PASSWORD "
                    + "para crearlo al arrancar.");
            return;
        }
        if (!Pattern.matches(ValidationRules.PASSWORD_PATTERN, password)) {
            log.error("SUPER_ADMIN_PASSWORD no cumple la política de contraseñas; no se creó el super administrador.");
            return;
        }
        if (!Pattern.matches(ValidationRules.USERNAME_PATTERN, username)) {
            log.error("SUPER_ADMIN_USERNAME no es válido; no se creó el super administrador.");
            return;
        }

        userRepository.findByEmailIgnoreCase(email).ifPresentOrElse(
                existing -> {
                    existing.setRole(Role.SUPER_ADMIN);
                    existing.setActive(true);
                    userRepository.save(existing);
                    log.info("El usuario {} fue promovido a super administrador.", email);
                },
                () -> {
                    if (userRepository.existsByUsernameIgnoreCase(username)) {
                        log.error("El username '{}' ya está en uso; no se creó el super administrador.", username);
                        return;
                    }
                    User superAdmin = User.newStudent("Super", "Administrador", username, email,
                            passwordEncoder.encode(password), null);
                    superAdmin.setRole(Role.SUPER_ADMIN);
                    userRepository.save(superAdmin);
                    log.info("Super administrador {} creado.", email);
                });
    }
}
