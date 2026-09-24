package co.uceva.submission.infrastructure.config;

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
 * Configuración de seguridad para el servicio de envíos de código fuente.
 * <p>
 * Habilita la seguridad a nivel de métodos mediante {@link EnableMethodSecurity},
 * lo que permite utilizar anotaciones como {@code @PreAuthorize} en los
 * controladores REST. {@link JwtAuthenticationFilter} valida el JWT y deja la
 * identidad y su rol en el contexto de seguridad antes de comprobar los permisos.
 * </p>
 * <p>
 * El canal WebSocket también autentica con JWT, mediante
 * {@code JwtHandshakeInterceptor} durante el handshake. El filtro HTTP y el
 * handshake comparten el validador declarado en {@link JwtConfig}.
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
     * - Registra el filtro JWT antes de {@code UsernamePasswordAuthenticationFilter}.<br>
     * - Permite cualquier request a nivel de cadena; los roles se exigen con
     *   {@code @PreAuthorize} en cada método del controller.<br>
     * </p>
     *
     * @param http           el configurador de seguridad HTTP de Spring.
     * @param tokenValidator validador de tokens declarado en {@link JwtConfig}.
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
