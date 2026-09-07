package co.uceva.auth.infrastructure.cache;

import co.uceva.auth.application.port.out.CachePort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisCacheAdapter implements CachePort {

    private final StringRedisTemplate redisTemplate;
    
    private static final String FAILED_ATTEMPTS_PREFIX = "auth:failed_attempts:";
    private static final String LOCK_PREFIX = "auth:lock:";
    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh_token:";

    public RedisCacheAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void incrementFailedAttempts(String email) {
        String key = FAILED_ATTEMPTS_PREFIX + email;
        redisTemplate.opsForValue().increment(key);
        // Optional: set expiration for failed attempts so they clear after some time
        redisTemplate.expire(key, Duration.ofHours(1));
    }

    @Override
    public void resetFailedAttempts(String email) {
        redisTemplate.delete(FAILED_ATTEMPTS_PREFIX + email);
    }

    @Override
    public int getFailedAttempts(String email) {
        String count = redisTemplate.opsForValue().get(FAILED_ATTEMPTS_PREFIX + email);
        return count != null ? Integer.parseInt(count) : 0;
    }

    @Override
    public void lockAccount(String email, long durationMinutes) {
        String lockKey = LOCK_PREFIX + email;
        redisTemplate.opsForValue().set(lockKey, "LOCKED", Duration.ofMinutes(durationMinutes));
    }

    @Override
    public boolean isAccountLocked(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(LOCK_PREFIX + email));
    }

    @Override
    public long getLockTimeLeftSeconds(String email) {
        Long expire = redisTemplate.getExpire(LOCK_PREFIX + email);
        return expire != null && expire > 0 ? expire : 0;
    }

    @Override
    public void saveRefreshToken(String userId, String token, long durationMillis) {
        redisTemplate.opsForValue().set(REFRESH_TOKEN_PREFIX + userId, token, Duration.ofMillis(durationMillis));
    }
}
