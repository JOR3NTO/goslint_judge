package co.uceva.shared.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Verifica tokens JWT emitidos por {@code auth-service} y los traduce a una
 * identidad utilizable por el resto del sistema.
 * <p>
 * Solo <em>valida</em>: no emite tokens ni gestiona sesiones. Cada servicio que
 * necesite reconocer a un usuario comparte esta misma clase, de modo que la
 * comprobación de firma, expiración y emisor se escribe una vez y no puede
 * divergir entre servicios.
 * </p>
 * <p>
 * No lleva anotaciones de Spring a propósito: {@code common-infrastructure} es
 * una librería y sus paquetes quedan fuera del escaneo de componentes de los
 * servicios. Cada servicio la declara como {@code @Bean} en su propia
 * configuración de seguridad.
 * </p>
 */
public class JwtTokenValidator {

    /** Claim que transporta el rol del usuario dentro del token. */
    private static final String ROLE_CLAIM = "role";

    /** Longitud mínima de la clave para HMAC-SHA256, exigida por la especificación. */
    private static final int MIN_SECRET_LENGTH_BYTES = 32;

    private final JwtParser parser;

    /**
     * @param secret Clave compartida con la que {@code auth-service} firma los tokens.
     * @param issuer Emisor esperado; un token emitido por cualquier otro se rechaza.
     * @throws IllegalArgumentException Si la clave es demasiado corta para HMAC-SHA256.
     */
    public JwtTokenValidator(String secret, String issuer) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_LENGTH_BYTES) {
            throw new IllegalArgumentException(
                    "La clave de firma JWT debe tener al menos %d bytes.".formatted(MIN_SECRET_LENGTH_BYTES));
        }
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.parser = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build();
    }

    /**
     * Comprueba el token y devuelve la identidad que contiene.
     * <p>
     * La firma, la expiración y el emisor se verifican antes de leer ningún
     * claim, de modo que nunca se confía en el contenido de un token que no haya
     * superado la validación.
     * </p>
     *
     * @param token Token JWT en su forma compacta, sin el prefijo {@code Bearer}.
     * @return La identidad del usuario al que pertenece el token.
     * @throws InvalidTokenException Si el token está ausente, mal formado, expirado,
     *                               firmado con otra clave o le faltan los claims mínimos.
     */
    public AuthenticatedUser validate(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("no se recibió ningún token");
        }

        Claims claims;
        try {
            claims = parser.parseSignedClaims(token).getPayload();
        } catch (Exception e) {
            throw new InvalidTokenException("la firma, el emisor o la vigencia no son válidos", e);
        }

        UUID userId = readUserId(claims);
        String role = claims.get(ROLE_CLAIM, String.class);
        if (role == null || role.isBlank()) {
            throw new InvalidTokenException("el token no declara el rol del usuario");
        }

        return new AuthenticatedUser(userId, role);
    }

    /**
     * Extrae el identificador del usuario del claim {@code sub}.
     *
     * @param claims Claims de un token ya verificado.
     * @return Identificador del usuario.
     * @throws InvalidTokenException Si el claim falta o no es un UUID.
     */
    private UUID readUserId(Claims claims) {
        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new InvalidTokenException("el token no identifica a ningún usuario");
        }
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            throw new InvalidTokenException("el identificador de usuario del token no es un UUID", e);
        }
    }
}
