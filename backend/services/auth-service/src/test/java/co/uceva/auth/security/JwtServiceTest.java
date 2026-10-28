package co.uceva.auth.security;

import co.uceva.auth.model.Role;
import co.uceva.auth.model.User;
import co.uceva.shared.infrastructure.security.InvalidTokenException;
import co.uceva.shared.infrastructure.security.JwtTokenValidator;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private static final String ISSUER = "test-issuer";

    private final JwtService jwtService = new JwtService(SECRET, ISSUER, 900_000, 28_800_000);
    private final User user = User.builder().id(UUID.randomUUID()).email("a@b.co").role(Role.STUDENT).build();

    @Test
    void accessTokenIsOnlyAcceptedAsAccessToken() {
        String token = jwtService.generateAccessToken(user);

        assertEquals(user.getId(), jwtService.parseAccessToken(token).orElseThrow());
        assertTrue(jwtService.parseRefreshToken(token).isEmpty());
    }

    @Test
    void refreshTokenIsNotAcceptedAsAccessToken() {
        String token = jwtService.generateRefreshToken(user);

        assertTrue(jwtService.parseAccessToken(token).isEmpty());
        assertEquals(user.getId(), jwtService.parseRefreshToken(token).orElseThrow());
    }

    @Test
    void tokensIssuedForTheSameUserAreDifferent() {
        assertNotEquals(jwtService.generateRefreshToken(user), jwtService.generateRefreshToken(user));
    }

    @Test
    void parseReportsRemainingValidity() {
        long remaining = jwtService.parse(jwtService.generateAccessToken(user)).orElseThrow().remainingMillis();

        assertTrue(remaining > 0 && remaining <= 900_000);
    }

    @Test
    void rejectsGarbageBlankAndNull() {
        assertTrue(jwtService.parse("no.es.jwt").isEmpty());
        assertTrue(jwtService.parse(" ").isEmpty());
        assertTrue(jwtService.parse(null).isEmpty());
    }

    @Test
    void rejectsExpiredToken() {
        JwtService shortLived = new JwtService(SECRET, ISSUER, -1_000, 1_000);

        assertTrue(jwtService.parse(shortLived.generateAccessToken(user)).isEmpty());
    }

    @Test
    void rejectsTokenFromAnotherIssuer() {
        JwtService other = new JwtService(SECRET, "otro-emisor", 900_000, 900_000);

        assertTrue(jwtService.parse(other.generateAccessToken(user)).isEmpty());
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() {
        String forged = Jwts.builder()
                .subject(user.getId().toString())
                .issuer(ISSUER)
                .claim("type", "access")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor("ffffffffffffffffffffffffffffffff".getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertTrue(jwtService.parse(forged).isEmpty());
    }

    @Test
    void sharedValidatorAcceptsAccessTokenAndRejectsRefreshToken() {
        JwtTokenValidator validator = new JwtTokenValidator(SECRET, ISSUER);

        assertEquals(user.getId(), validator.validate(jwtService.generateAccessToken(user)).userId());
        assertThrows(InvalidTokenException.class, () -> validator.validate(jwtService.generateRefreshToken(user)));
    }
}
