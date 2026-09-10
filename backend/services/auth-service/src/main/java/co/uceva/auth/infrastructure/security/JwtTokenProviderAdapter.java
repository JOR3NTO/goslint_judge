package co.uceva.auth.infrastructure.security;

import co.uceva.auth.domain.service.TokenProvider;
import co.uceva.auth.domain.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProviderAdapter implements TokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration.access}")
    private long jwtExpirationAccessMs;

    @Value("${jwt.expiration.refresh}")
    private long jwtExpirationRefreshMs;
    
    @Value("${jwt.issuer:goslint-auth-service}")
    private String jwtIssuer;

    @Override
    public String generateAccessToken(User user) {
        return generateToken(user, jwtExpirationAccessMs);
    }

    @Override
    public String generateRefreshToken(User user) {
        return generateToken(user, jwtExpirationRefreshMs);
    }

    @Override
    public Long getAccessTokenExpirationMs() {
        return jwtExpirationAccessMs;
    }

    private String generateToken(User user, long expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(user.getId().toString())
                .issuer(jwtIssuer)
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key())
                .compact();
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
