package co.uceva.problem.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad para el servicio de problemas.
 * <p>
 * Habilita la seguridad a nivel de métodos mediante {@link EnableMethodSecurity},
 * lo que permite utilizar anotaciones como {@code @PreAuthorize} en los
 * controladores REST. La validación real del JWT será implementada en el futuro;
 * por ahora todos los requests son permitidos a nivel de filtro HTTP para no
 * romper el comportamiento actual mientras se agregan las restricciones por rol.
 * </p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    /**
     * Configura la cadena de filtros de seguridad.
     * <p>
     * - Deshabilita CSRF porque se trata de una API stateless.<br>
     * - Configura la política de sesiones como STATELESS.<br>
     * - Permite cualquier request a nivel de filtro; la autorización se define
     *   con {@code @PreAuthorize} en cada método del controller.<br>
     * </p>
     *
     * @param http el configurador de seguridad HTTP de Spring.
     * @return la cadena de filtros configurada.
     * @throws Exception si ocurre un error al construir la cadena.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
