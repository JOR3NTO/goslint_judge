package co.uceva.problem.infrastructure.config;

import co.uceva.shared.infrastructure.security.JwtTokenValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración de seguridad para el servicio de problemas.
 * <p>
 * Habilita la seguridad a nivel de métodos mediante {@link EnableMethodSecurity},
 * lo que permite utilizar anotaciones como {@code @PreAuthorize} en los
 * controladores REST. La autenticación se delega a {@link JwtAuthenticationFilter},
 * que valida el token JWT de cada petición HTTP antes de que Spring Security evalúe
 * los permisos.
 * </p>
 * <p>
 * Los endpoints públicos (sin {@code @PreAuthorize}) siguen siendo accesibles sin
 * token: cuando la cabecera {@code Authorization} está ausente o el token es
 * inválido, el filtro no establece ninguna autenticación y la petición continúa
 * como anónima. Spring Security solo rechazará las peticiones que lleguen a un
 * endpoint que exija un rol que el principal anónimo no tiene.
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
     * - Registra {@link JwtAuthenticationFilter} antes de
     *   {@code UsernamePasswordAuthenticationFilter} para que cada petición
     *   quede autenticada con la identidad extraída del token JWT.<br>
     * - Permite cualquier request a nivel de cadena de filtros; la autorización
     *   efectiva la imponen las anotaciones {@code @PreAuthorize} de cada
     *   método del controller.<br>
     * </p>
     *
     * @param http           el configurador de seguridad HTTP de Spring.
     * @param tokenValidator validador de tokens JWT declarado en {@link JwtConfig}.
     * @return la cadena de filtros configurada.
     * @throws Exception si ocurre un error al construir la cadena.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            JwtTokenValidator tokenValidator) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new JwtAuthenticationFilter(tokenValidator),
                        UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
