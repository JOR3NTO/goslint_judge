package co.uceva.auth.security;

import co.uceva.auth.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Emite y lee los JWT de la plataforma.
 * <p>
 * Cada token lleva un claim {@code type} ({@code access} o {@code refresh}) para que un
 * refresh token, que vive 8 horas, nunca pueda usarse como credencial de acceso.
 * </p>
 */
@Service
public class JwtService {

    static final String TYPE_CLAIM = "type";
    static final String ACCESS = "access";
    static final String REFRESH = "refresh";

    /** Datos de un token con firma, emisor y vigencia válidos. */
    public record TokenInfo(UUID userId, String type, long remainingMillis) {
        public boolean isRefresh() {
            return REFRESH.equals(type);
        }
    }

    private final SecretKey key;
    private final JwtParser parser;
    private final String issuer;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.issuer:goslint-judge}") String issuer,
                      @Value("${jwt.expiration.access}") long accessExpirationMs,
                      @Value("${jwt.expiration.refresh}") long refreshExpirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.parser = Jwts.parser().verifyWith(key).requireIssuer(issuer).build();
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateAccessToken(User user) {
        return generate(user, ACCESS, accessExpirationMs);
    }

    public String generateRefreshToken(User user) {
        return generate(user, REFRESH, refreshExpirationMs);
    }

    public long getAccessExpirationMs() {
        return accessExpirationMs;
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

    /** Devuelve el id del usuario si el token es un access token válido. */
    public Optional<UUID> parseAccessToken(String rawToken) {
        return parse(rawToken).filter(info -> ACCESS.equals(info.type())).map(TokenInfo::userId);
    }

    /** Devuelve el id del usuario si el token es un refresh token válido. */
    public Optional<UUID> parseRefreshToken(String rawToken) {
        return parse(rawToken).filter(TokenInfo::isRefresh).map(TokenInfo::userId);
    }

    /**
     * Lee un token de cualquier tipo.
     *
     * @return vacío si está mal formado, expiró, tiene firma o emisor incorrectos o no
     *         identifica a un usuario.
     */
    public Optional<TokenInfo> parse(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = parser.parseSignedClaims(rawToken).getPayload();
            UUID userId = UUID.fromString(claims.getSubject());
            long remaining = Math.max(0, claims.getExpiration().getTime() - System.currentTimeMillis());
            return Optional.of(new TokenInfo(userId, claims.get(TYPE_CLAIM, String.class), remaining));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private String generate(User user, String type, long expirationMs) {
        Date now = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString()) // evita tokens idénticos emitidos en el mismo segundo
                .subject(user.getId().toString())
                .issuer(issuer)
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim(TYPE_CLAIM, type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }
}
