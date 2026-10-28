package co.uceva.auth.cache;

import co.uceva.shared.infrastructure.security.TokenBlacklist;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Estado de sesión en Redis: intentos fallidos de login, bloqueos de cuenta,
 * refresh tokens vigentes y lista negra de access tokens.
 * <p>
 * También es el {@link TokenBlacklist} que consultan los demás servicios.
 * </p>
 */
@Component
public class AuthCache implements TokenBlacklist {

    private static final String FAILED_ATTEMPTS_PREFIX = "auth:failed_attempts:";
    private static final String LOCK_PREFIX = "auth:lock:";
    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh_token:";
    private static final String BLACKLIST_PREFIX = "auth:blacklist:";

    private static final Duration FAILED_ATTEMPTS_WINDOW = Duration.ofHours(1);

    private final StringRedisTemplate redis;

    public AuthCache(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * Suma un intento fallido. La ventana de una hora se abre con el primero y no se
     * renueva con los siguientes.
     *
     * @return número de intentos fallidos acumulados, contando este.
     */
    public long registerFailedAttempt(String email) {
        String key = FAILED_ATTEMPTS_PREFIX + email;
        Long attempts = redis.opsForValue().increment(key);
        if (attempts != null && attempts == 1) {
            redis.expire(key, FAILED_ATTEMPTS_WINDOW);
        }
        return attempts == null ? 0 : attempts;
    }

    public void resetFailedAttempts(String email) {
        redis.delete(FAILED_ATTEMPTS_PREFIX + email);
    }

    public void lockAccount(String email, long minutes) {
        redis.opsForValue().set(LOCK_PREFIX + email, "LOCKED", Duration.ofMinutes(minutes));
    }

    public boolean isAccountLocked(String email) {
        return Boolean.TRUE.equals(redis.hasKey(LOCK_PREFIX + email));
    }

    public long getLockSecondsLeft(String email) {
        Long seconds = redis.getExpire(LOCK_PREFIX + email);
        return seconds != null && seconds > 0 ? seconds : 0;
    }

    public void saveRefreshToken(String userId, String token, long ttlMillis) {
        redis.opsForValue().set(REFRESH_TOKEN_PREFIX + userId, token, Duration.ofMillis(ttlMillis));
    }

    /** @return el refresh token vigente del usuario, o {@code null} si no tiene sesión. */
    public String getRefreshToken(String userId) {
        return redis.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);
    }

    public void deleteRefreshToken(String userId) {
        redis.delete(REFRESH_TOKEN_PREFIX + userId);
    }

    /** Revoca un access token hasta el momento en que habría expirado. */
    public void blacklistToken(String rawToken, long ttlMillis) {
        redis.opsForValue().set(BLACKLIST_PREFIX + sha256(rawToken), "REVOKED", Duration.ofMillis(ttlMillis));
    }

    @Override
    public boolean isBlacklisted(String rawToken) {
        return Boolean.TRUE.equals(redis.hasKey(BLACKLIST_PREFIX + sha256(rawToken)));
    }

    /** Se guarda el hash del token, no el token, para no llenar Redis de claves largas ni credenciales. */
    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
