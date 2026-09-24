package co.uceva.problem.infrastructure.config;

import co.uceva.shared.infrastructure.security.AuthenticatedUser;
import co.uceva.shared.infrastructure.security.InvalidTokenException;
import co.uceva.shared.infrastructure.security.JwtTokenValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro de autenticación basado en JWT para peticiones HTTP entrantes.
 * <p>
 * Intercepta cada petición una sola vez, extrae el token del encabezado
 * {@code Authorization: Bearer <token>}, lo valida mediante
 * {@link JwtTokenValidator} y publica la identidad resultante en el
 * {@code SecurityContext}, donde {@code @PreAuthorize} la leerá para decidir
 * si el usuario tiene el rol exigido.
 * </p>
 * <p>
 * Cuando la cabecera está ausente o el token no supera la validación, el filtro
 * no interrumpe la cadena: simplemente no establece ninguna autenticación.
 * Si el endpoint requiere un rol concreto, Spring Security devolverá {@code 401}
 * automáticamente al llegar a la capa de autorización con un principal anónimo.
 * Este diseño permite que los endpoints públicos (sin {@code @PreAuthorize})
 * sigan siendo accesibles sin token.
 * </p>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenValidator tokenValidator;

    /**
     * @param tokenValidator Validador de tokens JWT compartido con otros filtros
     *                       o interceptores del mismo servicio.
     */
    public JwtAuthenticationFilter(JwtTokenValidator tokenValidator) {
        this.tokenValidator = tokenValidator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String rawToken = header.substring(BEARER_PREFIX.length()).strip();
            authenticate(rawToken);
        }

        chain.doFilter(request, response);
    }

    /**
     * Valida el token y, si es válido, registra la identidad en el contexto de
     * seguridad. Si el token es rechazado, no se hace nada y la petición continúa
     * como anónima.
     *
     * @param rawToken Token JWT sin el prefijo {@code Bearer}.
     */
    private void authenticate(String rawToken) {
        try {
            AuthenticatedUser user = tokenValidator.validate(rawToken);
            List<SimpleGrantedAuthority> authorities =
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.role()));
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(user, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (InvalidTokenException ignored) {
            // Token inválido → la petición sigue como anónima; Spring Security
            // rechazará el acceso a los endpoints protegidos con @PreAuthorize.
        }
    }
}
