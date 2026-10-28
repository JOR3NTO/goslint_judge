package co.uceva.auth.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthCacheTest {

    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> values;

    private AuthCache cache;

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForValue()).thenReturn(values);
        cache = new AuthCache(redis);
    }

    @Test
    void firstFailedAttemptOpensTheTimeWindow() {
        when(values.increment("auth:failed_attempts:a@b.co")).thenReturn(1L);

        assertEquals(1, cache.registerFailedAttempt("a@b.co"));
        verify(redis).expire("auth:failed_attempts:a@b.co", Duration.ofHours(1));
    }

    @Test
    void laterFailedAttemptsDoNotRenewTheWindow() {
        when(values.increment("auth:failed_attempts:a@b.co")).thenReturn(3L);

        assertEquals(3, cache.registerFailedAttempt("a@b.co"));
        verify(redis, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void lockIsReportedWithRemainingSeconds() {
        when(redis.hasKey("auth:lock:a@b.co")).thenReturn(true);
        when(redis.getExpire("auth:lock:a@b.co")).thenReturn(120L);

        assertTrue(cache.isAccountLocked("a@b.co"));
        assertEquals(120, cache.getLockSecondsLeft("a@b.co"));
    }

    @Test
    void missingLockHasZeroSecondsLeft() {
        when(redis.getExpire("auth:lock:a@b.co")).thenReturn(-2L);

        assertEquals(0, cache.getLockSecondsLeft("a@b.co"));
    }

    @Test
    void refreshTokenRoundTrip() {
        cache.saveRefreshToken("u1", "tok", 5_000);
        verify(values).set("auth:refresh_token:u1", "tok", Duration.ofMillis(5_000));

        when(values.get("auth:refresh_token:u1")).thenReturn("tok");
        assertEquals("tok", cache.getRefreshToken("u1"));

        cache.deleteRefreshToken("u1");
        verify(redis).delete("auth:refresh_token:u1");
    }

    @Test
    void blacklistStoresAHashNeverTheRawToken() {
        cache.blacklistToken("header.payload.signature", 60_000);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(values).set(key.capture(), eq("REVOKED"), eq(Duration.ofMillis(60_000)));
        assertTrue(key.getValue().startsWith("auth:blacklist:"));
        assertFalse(key.getValue().contains("header.payload.signature"));
        assertEquals("auth:blacklist:".length() + 64, key.getValue().length());
    }

    @Test
    void blacklistLookupUsesTheSameHashAsTheWrite() {
        ArgumentCaptor<String> written = ArgumentCaptor.forClass(String.class);
        cache.blacklistToken("tok", 1_000);
        verify(values).set(written.capture(), anyString(), any(Duration.class));

        when(redis.hasKey(written.getValue())).thenReturn(true);

        assertTrue(cache.isBlacklisted("tok"));
        assertFalse(cache.isBlacklisted("otro"));
    }
}
