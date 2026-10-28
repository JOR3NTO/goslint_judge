package co.uceva.shared.infrastructure.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenValidatorTokenTypeTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private static final String ISSUER = "test-issuer";

    private final JwtTokenValidator validator = new JwtTokenValidator(SECRET, ISSUER);

    private String token(UUID userId, String type) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        var builder = Jwts.builder()
                .subject(userId.toString())
                .issuer(ISSUER)
                .claim("role", "STUDENT")
                .expiration(new Date(System.currentTimeMillis() + 60_000));
        if (type != null) {
            builder.claim("type", type);
        }
        return builder.signWith(key).compact();
    }

    @Test
    void rejectsRefreshToken() {
        String refresh = token(UUID.randomUUID(), "refresh");

        assertThrows(InvalidTokenException.class, () -> validator.validate(refresh));
    }

    @Test
    void acceptsAccessToken() {
        UUID id = UUID.randomUUID();

        assertEquals(id, validator.validate(token(id, "access")).userId());
    }

    @Test
    void acceptsTokenWithoutTypeClaim() {
        UUID id = UUID.randomUUID();

        assertEquals(id, validator.validate(token(id, null)).userId());
    }
}
