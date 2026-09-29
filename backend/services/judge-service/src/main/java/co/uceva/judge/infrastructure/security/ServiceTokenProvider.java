package co.uceva.judge.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Emite el JWT con el que {@code judge-service} se identifica ante otros
 * servicios (hoy, {@code problem-service}) para leer los casos de prueba
 * privados.
 * <p>
 * El juez no es un usuario humano y no pasa por {@code auth-service}: firma su
 * propio token con la clave compartida, con {@code role=SERVICE}, que es el rol
 * técnico previsto para la comunicación servicio a servicio. Los claims son los
 * que espera {@code JwtTokenValidator}: {@code sub} (UUID), {@code role},
 * emisor y vigencia.
 * </p>
 * <p>
 * El token se reutiliza mientras le quede vigencia, para no firmar uno nuevo en
 * cada petición.
 * </p>
 */
@Component
public class ServiceTokenProvider {

    /** Rol técnico de comunicación servicio a servicio. */
    static final String SERVICE_ROLE = "SERVICE";
    /** Margen antes de la expiración a partir del cual se emite un token nuevo. */
    private static final Duration REFRESH_MARGIN = Duration.ofSeconds(30);
    private static final int MIN_SECRET_LENGTH_BYTES = 32;

    private final SecretKey key;
    private final String issuer;
    private final UUID serviceId;
    private final Duration ttl;
    private final Clock clock;

    private String cachedToken;
    private Instant cachedExpiration = Instant.MIN;

    /**
     * @param secret     Clave compartida con la que se firman y verifican los tokens.
     * @param issuer     Emisor que declaran los tokens; debe coincidir con el que esperan los servicios.
     * @param serviceId  Identificador (claim {@code sub}) con el que se identifica este servicio.
     * @param ttlSeconds Vigencia de cada token, en segundos.
     */
    @Autowired
    public ServiceTokenProvider(@Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.issuer}") String issuer,
            @Value("${app.security.jwt.service-id}") UUID serviceId,
            @Value("${app.security.jwt.service-token-ttl-seconds:300}") long ttlSeconds) {
        this(secret, issuer, serviceId, Duration.ofSeconds(ttlSeconds), Clock.systemUTC());
    }

    ServiceTokenProvider(String secret, String issuer, UUID serviceId, Duration ttl, Clock clock) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_LENGTH_BYTES) {
            throw new IllegalArgumentException(
                    "La clave de firma JWT debe tener al menos %d bytes.".formatted(MIN_SECRET_LENGTH_BYTES));
        }
        if (ttl.compareTo(REFRESH_MARGIN) <= 0) {
            throw new IllegalArgumentException("La vigencia del token debe superar " + REFRESH_MARGIN.toSeconds() + " s.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.serviceId = serviceId;
        this.ttl = ttl;
        this.clock = clock;
    }

    /**
     * @return Un token vigente con {@code role=SERVICE}, en su forma compacta y sin el prefijo {@code Bearer}.
     */
    public synchronized String token() {
        Instant now = clock.instant();
        if (cachedToken == null || !now.isBefore(cachedExpiration.minus(REFRESH_MARGIN))) {
            cachedExpiration = now.plus(ttl);
            cachedToken = Jwts.builder()
                    .issuer(issuer)
                    .subject(serviceId.toString())
                    .claim("role", SERVICE_ROLE)
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(cachedExpiration))
                    .signWith(key)
                    .compact();
        }
        return cachedToken;
    }
}
