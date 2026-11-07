package co.uceva.contests_platform.auth.config;

import co.uceva.shared.infrastructure.security.JwtTokenValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración de seguridad común a todos los módulos del monolito modular.
 * <p>
 * Hay una sola cadena de filtros para toda la aplicación: el filtro JWT
 * autentica cada petición cuando trae un token válido y las anotaciones
 * {@code @PreAuthorize} de cada endpoint imponen la autorización efectiva.
 * Los endpoints públicos siguen accesibles sin token.
 * </p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    /** Orígenes CORS permitidos, separados por coma (propiedad {@code cors.allowed-origins}). */
    @Value("${cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    /**
     * Cadena única de filtros para la aplicación.
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
     * @param tokenValidator validador de tokens JWT común a los módulos.
     * @return la cadena de filtros configurada.
     * @throws Exception si ocurre un error al construir la cadena.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            JwtTokenValidator tokenValidator) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new JwtAuthenticationFilter(tokenValidator),
                        UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    /**
     * Validador de los tokens emitidos por el módulo de autenticación.
     * <p>
     * La clave no tiene valor por defecto a propósito: un secreto de ejemplo
     * heredado sin querer en producción aceptaría tokens falsificados por
     * cualquiera que conociese el repositorio. Sin la variable de entorno, el
     * servicio no arranca.
     * </p>
     *
     * @param secret Clave compartida con la que se firman los tokens.
     * @param issuer Emisor esperado en los tokens.
     * @return Validador listo para verificar tokens.
     */
    @Bean
    public JwtTokenValidator jwtTokenValidator(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.issuer}") String issuer) {
        return new JwtTokenValidator(secret, issuer);
    }

    /**
     * Orígenes, métodos y cabeceras permitidos para las peticiones del frontend.
     *
     * @return configuración CORS aplicada a todas las rutas.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "Authorization"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Codificador de contraseñas de los usuarios del módulo de autenticación.
     *
     * @return codificador BCrypt.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
