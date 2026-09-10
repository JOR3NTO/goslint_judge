package co.uceva.auth.domain.repository;

public interface AuthCacheRepository {
    void incrementFailedAttempts(String email);
    void resetFailedAttempts(String email);
    int getFailedAttempts(String email);
    void lockAccount(String email, long durationMinutes);
    boolean isAccountLocked(String email);
    long getLockTimeLeftSeconds(String email);
    void saveRefreshToken(String userId, String token, long durationMillis);
}
