package co.uceva.auth.infrastructure.config;

import co.uceva.auth.application.port.out.PasswordEncoderPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

/**
 * Clase de Configuración de Spring Security.
 * Define qué rutas son públicas, cuáles requieren autenticación
 * y establece los algoritmos de encriptación para contraseñas.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configura el filtro de seguridad de Spring.
     * Permite el acceso sin token a la ruta de registro.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // Deshabilita CSRF (Cross-Site Request Forgery) ya que usaremos APIs Stateless
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/register").permitAll() // Público para todos
                .anyRequest().authenticated() // Cualquier otra ruta requiere token (Implementación de Login pendiente)
            );
        return http.build();
    }

    /**
     * Bean de Spring que provee la implementación concreta de encriptación.
     * Utilizamos BCrypt, estándar actual para hashing de contraseñas.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Adaptador interno que envuelve el PasswordEncoder de Spring
     * y lo expone como el PasswordEncoderPort de la capa de aplicación.
     * Así, el dominio no depende directamente de org.springframework.security.
     */
    @Component
    public static class BCryptPasswordEncoderAdapter implements PasswordEncoderPort {
        // Inyección de dependencias del PasswordEncoder de Spring.
        private final PasswordEncoder passwordEncoder;

        /**
         * Constructor de la clase.
         * @param passwordEncoder Implementación concreta de PasswordEncoder de Spring.
         */
        public BCryptPasswordEncoderAdapter(PasswordEncoder passwordEncoder) {
            this.passwordEncoder = passwordEncoder;
        }

        /**
         * Encripta la contraseña usando BCrypt.
         * @param rawPassword Contraseña en texto plano.
         * @return Contraseña encriptada.
         */
        @Override
        public String encode(String rawPassword) {
            return passwordEncoder.encode(rawPassword);
        }

        /**
         * Verifica si la contraseña en texto plano coincide con la encriptada.
         * @param rawPassword Contraseña en texto plano.
         * @param encodedPassword Contraseña encriptada.
         * @return true si la contraseña coincide, false en caso contrario.
         */
        @Override
        public boolean matches(String rawPassword, String encodedPassword) {
            return passwordEncoder.matches(rawPassword, encodedPassword);
        }
    }
}
