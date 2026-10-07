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
    void blacklistsTokenWithRemainingLifetimeAsTtl() {
        long fiveMinutes = 5 * 60 * 1000L;
        when(tokenProvider.getRemainingValidityMillis("token")).thenReturn(fiveMinutes);

        useCase.logout("token");

        verify(cache).blacklistToken("token", fiveMinutes);
    }

    @Test
    void doesNothingWhenTokenAlreadyExpired() {
        when(tokenProvider.getRemainingValidityMillis("token")).thenReturn(0L);

        useCase.logout("token");

        verify(cache, never()).blacklistToken(anyString(), anyLong());
    }

    @Test
    void ignoresNullOrBlankToken() {
        useCase.logout(null);
        useCase.logout("  ");

        verifyNoInteractions(cache, tokenProvider);
    }
}
