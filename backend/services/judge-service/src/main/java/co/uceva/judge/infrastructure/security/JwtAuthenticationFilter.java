package co.uceva.judge.infrastructure.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import co.uceva.shared.infrastructure.security.AuthenticatedUser;
import co.uceva.shared.infrastructure.security.InvalidTokenException;
import co.uceva.shared.infrastructure.security.JwtTokenValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Autentica las peticiones HTTP a partir del JWT de la cabecera
 * {@code Authorization: Bearer}, con el mismo {@link JwtTokenValidator} que usan
 * los demás servicios. El claim {@code role} pasa a la autoridad {@code ROLE_<rol>}
 * que evalúa {@code @PreAuthorize}.
 * <p>
 * Un token ausente o inválido no rechaza la petición aquí: la deja anónima y es
 * la cadena de seguridad la que responde {@code 401}.
 * </p>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenValidator validator;

    /**
     * @param validator Validador de los tokens emitidos con la clave compartida.
     */
    public JwtAuthenticationFilter(JwtTokenValidator validator) {
        this.validator = validator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            try {
                AuthenticatedUser user = validator.validate(header.substring(BEARER_PREFIX.length()));
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.role()))));
            } catch (InvalidTokenException e) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
