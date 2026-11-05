package co.uceva.judge.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import co.uceva.judge.infrastructure.security.JwtAuthenticationFilter;
import co.uceva.shared.infrastructure.security.JwtTokenValidator;

/**
 * Seguridad HTTP de {@code judge-service}: API stateless con JWT. Solo el health
 * es público; el resto exige autenticación y cada endpoint declara su rol con
 * {@code @PreAuthorize}.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    /**
     * Validador de los tokens firmados con la clave compartida con {@code auth-service}.
     * <p>
     * Sin valor por defecto en la clave a propósito: un secreto de ejemplo heredado
     * en producción aceptaría tokens falsificados.
     * </p>
     *
     * @param secret Clave compartida de firma.
     * @param issuer Emisor esperado en los tokens.
     * @return Validador listo para verificar tokens.
     */
    @Bean
    public JwtTokenValidator jwtTokenValidator(@Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.issuer}") String issuer) {
        return new JwtTokenValidator(secret, issuer);
    }

    /**
     * @param http      Configurador de seguridad HTTP de Spring.
     * @param validator Validador de tokens.
     * @return La cadena de filtros: CSRF y sesiones desactivados, health público y el resto autenticado.
     * @throws Exception Si falla la construcción de la cadena.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenValidator validator) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(validator), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
