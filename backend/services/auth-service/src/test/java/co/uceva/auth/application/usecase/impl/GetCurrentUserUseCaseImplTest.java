package co.uceva.auth.application.usecase.impl;

import co.uceva.auth.domain.exception.AccountDisabledException;
import co.uceva.auth.domain.exception.BadCredentialsException;
import co.uceva.auth.domain.model.Role;
import co.uceva.auth.domain.model.User;
import co.uceva.auth.domain.repository.AuthCacheRepository;
import co.uceva.auth.domain.repository.UserRepository;
import co.uceva.auth.domain.service.TokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetCurrentUserUseCaseImplTest {

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private AuthCacheRepository authCacheRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GetCurrentUserUseCaseImpl useCase;

    private User sampleUser;
    private final String validToken = "valid.jwt.token";
    private final UUID sampleUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(sampleUserId)
                .firstName("Camila")
                .lastName("Restrepo")
                .username("camila_dev")
                .email("camila@uceva.edu.co")
                .role(Role.STUDENT)
                .institution("UCEVA")
                .isActive(true)
                .build();
    }

    @Test
    void returnsUserWhenTokenIsValidAndAccountIsActive() {
        when(authCacheRepository.isTokenBlacklisted(validToken)).thenReturn(false);
        when(tokenProvider.getUserIdFromToken(validToken)).thenReturn(sampleUserId.toString());
        when(userRepository.findById(sampleUserId)).thenReturn(Optional.of(sampleUser));

        User result = useCase.getCurrentUser(validToken);

        assertNotNull(result);
        assertEquals(sampleUserId, result.getId());
        assertEquals("camila_dev", result.getUsername());
        verify(userRepository).findById(sampleUserId);
    }

    @Test
    void throwsBadCredentialsWhenTokenIsNull() {
        assertThrows(BadCredentialsException.class, () -> useCase.getCurrentUser(null));
    }

    @Test
    void throwsBadCredentialsWhenTokenIsBlacklisted() {
        when(authCacheRepository.isTokenBlacklisted(validToken)).thenReturn(true);

        assertThrows(BadCredentialsException.class, () -> useCase.getCurrentUser(validToken));
    }

    @Test
    void throwsBadCredentialsWhenUserIdCannotBeExtracted() {
        when(authCacheRepository.isTokenBlacklisted(validToken)).thenReturn(false);
        when(tokenProvider.getUserIdFromToken(validToken)).thenReturn(null);

        assertThrows(BadCredentialsException.class, () -> useCase.getCurrentUser(validToken));
    }

    @Test
    void throwsBadCredentialsWhenUserNotFound() {
        when(authCacheRepository.isTokenBlacklisted(validToken)).thenReturn(false);
        when(tokenProvider.getUserIdFromToken(validToken)).thenReturn(sampleUserId.toString());
        when(userRepository.findById(sampleUserId)).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> useCase.getCurrentUser(validToken));
    }

    @Test
    void throwsAccountDisabledWhenUserIsInactive() {
        sampleUser.setActive(false);
        when(authCacheRepository.isTokenBlacklisted(validToken)).thenReturn(false);
        when(tokenProvider.getUserIdFromToken(validToken)).thenReturn(sampleUserId.toString());
        when(userRepository.findById(sampleUserId)).thenReturn(Optional.of(sampleUser));

        assertThrows(AccountDisabledException.class, () -> useCase.getCurrentUser(validToken));
    }
}
