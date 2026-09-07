package co.uceva.auth.infrastructure.security;

import co.uceva.auth.application.port.out.TokenProviderPort;
import co.uceva.auth.domain.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProviderAdapter implements TokenProviderPort {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration.access}")
    private long jwtExpirationAccessMs;

    @Value("${jwt.expiration.refresh}")
    private long jwtExpirationRefreshMs;

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
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key())
                .compact();
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }
}
