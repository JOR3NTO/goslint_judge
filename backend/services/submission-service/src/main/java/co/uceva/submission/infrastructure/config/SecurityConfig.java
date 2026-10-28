package co.uceva.submission.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad para el servicio de envíos de código fuente.
 * <p>
 * Habilita la seguridad a nivel de métodos mediante {@link EnableMethodSecurity},
 * lo que permite utilizar anotaciones como {@code @PreAuthorize} en los
 * controladores REST. La validación real del JWT será implementada en el futuro;
 * por ahora todos los requests son permitidos a nivel de filtro HTTP para no
 * romper el comportamiento actual mientras se agregan las restricciones por rol.
 * </p>
 * <p>
 * El canal WebSocket es la excepción y ya autentica de verdad, pero no a través de
 * esta cadena: lo hace {@code JwtHandshakeInterceptor}, que valida el token durante
 * el handshake y rechaza la conexión antes de aceptarla. Un canal que empuja datos
 * de un usuario concreto no puede quedar abierto esperando a que el filtro HTTP
 * llegue en una historia futura. El validador que ambos compartirán se declara en
 * {@link JwtConfig}.
 * </p>
 * <p>
 * <strong>Bypass temporal:</strong> con {@code app.security.bypass-auth=true} se
 * registra {@link TemporaryAuthBypassFilter}, que autentica cualquier petición
 * REST con todos los roles del sistema. Sin ese filtro, hoy no hay forma de que
 * {@code @PreAuthorize} deje pasar una petición real: la autenticación queda
 * anónima y todo responde {@code 403}. Por defecto la propiedad es
 * {@code false}; solo debe activarse en entornos de prueba locales, nunca en uno
 * real, y se retira en cuanto exista el filtro JWT de verdad para peticiones HTTP.
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
     * @param http        el configurador de seguridad HTTP de Spring.
     * @param bypassAuth  si se registra el bypass temporal de autenticación.
     * @return la cadena de filtros configurada.
     * @throws Exception si ocurre un error al construir la cadena.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            @Value("${app.security.bypass-auth:false}") boolean bypassAuth) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        if (bypassAuth) {
            http.addFilterBefore(new TemporaryAuthBypassFilter(), UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }
}
