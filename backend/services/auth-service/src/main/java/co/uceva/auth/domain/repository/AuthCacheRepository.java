package co.uceva.auth.domain.repository;

public interface AuthCacheRepository {
    void incrementFailedAttempts(String email);
    void resetFailedAttempts(String email);
    int getFailedAttempts(String email);
    void lockAccount(String email, long durationMinutes);
    boolean isAccountLocked(String email);
    long getLockTimeLeftSeconds(String email);
    void saveRefreshToken(String userId, String token, long durationMillis);

    /**
     * Elimina el refresh token de un usuario del store de sesiones.
     * Se invoca durante el logout para que el token de larga duración
     * tampoco pueda reutilizarse tras cerrar sesión.
     *
     * @param userId Identificador del usuario (UUID en formato String).
     */
    void deleteRefreshToken(String userId);

    /**
     * Revoca un access token insertándolo en la lista negra hasta que expire de forma natural.
     *
     * @param rawToken  Token JWT compacto, sin el prefijo Bearer.
     * @param ttlMillis Tiempo de vida restante del token en milisegundos.
     */
    void blacklistToken(String rawToken, long ttlMillis);

    /**
     * @param rawToken Token JWT compacto, sin el prefijo Bearer.
     * @return {@code true} si el token fue revocado y aún permanece en la lista negra.
     */
    boolean isTokenBlacklisted(String rawToken);
}
