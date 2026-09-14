package co.uceva.submission.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * BYPASS TEMPORAL DE AUTENTICACIÓN HTTP — SOLO PARA PRUEBAS LOCALES.
 * <p>
 * Autentica cada petición REST como un usuario con todos los roles del
 * sistema, para que las anotaciones {@code @PreAuthorize} de
 * {@code SubmissionController} dejen de rechazar todo con {@code 403}
 * mientras no exista un filtro JWT real para peticiones HTTP (hoy solo el
 * handshake del WebSocket, en {@code JwtHandshakeInterceptor}, valida un
 * token de verdad).
 * </p>
 * <p>
 * Solo se registra cuando {@code app.security.bypass-auth=true}
 * ({@link SecurityConfig}), que por defecto es {@code false}. Un token
 * {@code Authorization} enviado en la petición se ignora por completo: este
 * filtro no lo valida ni lo lee, así que su presencia o ausencia no cambia
 * nada mientras el bypass esté activo.
 * </p>
 * <p>
 * <strong>Borrar esta clase y la propiedad {@code app.security.bypass-auth}
 * en cuanto exista el filtro JWT real para peticiones HTTP.</strong>
 * </p>
 */
public class TemporaryAuthBypassFilter extends OncePerRequestFilter {

    private static final List<SimpleGrantedAuthority> ALL_ROLES = List.of(
            new SimpleGrantedAuthority("ROLE_STUDENT"),
            new SimpleGrantedAuthority("ROLE_ADMIN"),
            new SimpleGrantedAuthority("ROLE_ORGANIZER"),
            new SimpleGrantedAuthority("ROLE_SERVICE"));

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("bypass-auth-local", null, ALL_ROLES));
        chain.doFilter(request, response);
    }
}
