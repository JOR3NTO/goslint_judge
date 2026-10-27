package co.uceva.auth.application.usecase.impl;

import co.uceva.auth.domain.repository.AuthCacheRepository;
import co.uceva.auth.domain.service.TokenProvider;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LogoutUserUseCaseImplTest {

    private final AuthCacheRepository cache = mock(AuthCacheRepository.class);
    private final TokenProvider tokenProvider = mock(TokenProvider.class);
    private final LogoutUserUseCaseImpl useCase = new LogoutUserUseCaseImpl(cache, tokenProvider);

    @Test
    void blacklistsAccessTokenAndDeletesRefreshToken() {
        long fiveMinutes = 5 * 60 * 1000L;
        when(tokenProvider.getRemainingValidityMillis("token")).thenReturn(fiveMinutes);
        when(tokenProvider.getUserIdFromToken("token")).thenReturn("user-uuid-123");

        useCase.logout("token");

        verify(cache).blacklistToken("token", fiveMinutes);
        verify(cache).deleteRefreshToken("user-uuid-123");
    }

    @Test
    void blacklistsAccessTokenEvenWhenUserIdCannotBeExtracted() {
        long fiveMinutes = 5 * 60 * 1000L;
        when(tokenProvider.getRemainingValidityMillis("token")).thenReturn(fiveMinutes);
        when(tokenProvider.getUserIdFromToken("token")).thenReturn(null);

        useCase.logout("token");

        verify(cache).blacklistToken("token", fiveMinutes);
        verify(cache, never()).deleteRefreshToken(anyString());
    }

    @Test
    void doesNothingWhenTokenAlreadyExpired() {
        when(tokenProvider.getRemainingValidityMillis("token")).thenReturn(0L);

        useCase.logout("token");

        verify(cache, never()).blacklistToken(anyString(), anyLong());
        verify(cache, never()).deleteRefreshToken(anyString());
    }

    @Test
    void ignoresNullOrBlankToken() {
        useCase.logout(null);
        useCase.logout("  ");

        verifyNoInteractions(cache, tokenProvider);
    }
}
