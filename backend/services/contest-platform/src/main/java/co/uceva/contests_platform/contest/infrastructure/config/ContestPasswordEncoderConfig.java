package co.uceva.contests_platform.contest.infrastructure.config;

import co.uceva.contests_platform.contest.domain.service.PasswordEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Provee el codificador de contraseñas usado por el dominio de concursos.
 */
@Configuration
public class ContestPasswordEncoderConfig {

    /**
     * Implementación del puerto de dominio {@link PasswordEncoder} con BCrypt,
     * usada para cifrar la contraseña de inscripción de los concursos.
     *
     * @return Codificador de contraseñas basado en BCrypt.
     */
    @Bean
    public PasswordEncoder contestPasswordEncoder() {
        return new PasswordEncoder() {
            private final org.springframework.security.crypto.password.PasswordEncoder delegate =
                    new BCryptPasswordEncoder();

            @Override
            public String encode(String rawPassword) {
                return delegate.encode(rawPassword);
            }

            @Override
            public boolean matches(String rawPassword, String encodedPassword) {
                return delegate.matches(rawPassword, encodedPassword);
            }
        };
    }
}
