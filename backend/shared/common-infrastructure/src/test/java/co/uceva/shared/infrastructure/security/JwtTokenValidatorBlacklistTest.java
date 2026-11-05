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

class JwtTokenValidatorBlacklistTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private static final String ISSUER = "test-issuer";

    private String token(UUID userId) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(userId.toString())
                .issuer(ISSUER)
                .claim("role", "STUDENT")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key)
                .compact();
    }

    @Test
    void rejectsBlacklistedToken() {
        JwtTokenValidator validator = new JwtTokenValidator(SECRET, ISSUER, t -> true);

        assertThrows(InvalidTokenException.class, () -> validator.validate(token(UUID.randomUUID())));
    }

    @Test
    void acceptsValidTokenNotInBlacklist() {
        UUID id = UUID.randomUUID();
        JwtTokenValidator validator = new JwtTokenValidator(SECRET, ISSUER, t -> false);

        assertEquals(id, validator.validate(token(id)).userId());
    }

    @Test
    void worksWithoutBlacklist() {
        UUID id = UUID.randomUUID();
        JwtTokenValidator validator = new JwtTokenValidator(SECRET, ISSUER);

        assertEquals(id, validator.validate(token(id)).userId());
    }
}
