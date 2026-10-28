package co.uceva.auth.service;

import co.uceva.auth.cache.AuthCache;
import co.uceva.auth.dto.LoginResponse;
import co.uceva.auth.dto.RegisterRequest;
import co.uceva.auth.exception.AccountDisabledException;
import co.uceva.auth.exception.AccountLockedException;
import co.uceva.auth.exception.BadCredentialsException;
import co.uceva.auth.exception.UserAlreadyExistsException;
import co.uceva.auth.model.Role;
import co.uceva.auth.model.User;
import co.uceva.auth.repository.UserRepository;
import co.uceva.auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthCache authCache;

    private final JwtService jwtService = new JwtService(SECRET, "test", 900_000, 28_800_000);
    private AuthService authService;
    private User user;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, authCache, jwtService);
        user = User.builder()
                .id(UUID.randomUUID())
                .username("camila")
                .email("camila@uceva.edu.co")
                .passwordHash("hash")
                .role(Role.STUDENT)
                .active(true)
                .build();
    }

    // ----------------------------------------------------------------- register

    private RegisterRequest registerRequest() {
        return new RegisterRequest("Camila", "Restrepo", " camila ", " Camila@UCEVA.edu.co ", "Secreta1!", "UCEVA");
    }

    @Test
    void registerStoresNormalizedEmailAndHashedPassword() {
        when(userRepository.existsByEmailIgnoreCase("camila@uceva.edu.co")).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase("camila")).thenReturn(false);
        when(passwordEncoder.encode("Secreta1!")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = authService.register(registerRequest());

        assertEquals("camila@uceva.edu.co", saved.getEmail());
        assertEquals("camila", saved.getUsername());
        assertEquals("hash", saved.getPasswordHash());
        assertEquals(Role.STUDENT, saved.getRole());
        assertTrue(saved.isActive());
    }

    @Test
    void registerRejectsTakenEmail() {
        when(userRepository.existsByEmailIgnoreCase("camila@uceva.edu.co")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(registerRequest()));
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerRejectsTakenUsername() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase("camila")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(registerRequest()));
        verify(userRepository, never()).save(any());
    }

    // -------------------------------------------------------------------- login

    @Test
    void loginIssuesTokensAndStoresRefreshToken() {
        when(authCache.isAccountLocked("camila@uceva.edu.co")).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase("camila@uceva.edu.co")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Secreta1!", "hash")).thenReturn(true);

        LoginResponse response = authService.login(" Camila@Uceva.edu.co ", "Secreta1!");

        assertEquals("Bearer", response.type());
        assertEquals(900_000, response.expiresIn());
        assertEquals(user.getId(), jwtService.parseAccessToken(response.accessToken()).orElseThrow());
        assertEquals(user.getId(), jwtService.parseRefreshToken(response.refreshToken()).orElseThrow());
        verify(authCache).saveRefreshToken(user.getId().toString(), response.refreshToken(), 28_800_000);
        verify(authCache).resetFailedAttempts("camila@uceva.edu.co");
    }

    @Test
    void loginFailsWithWrongPasswordAndCountsAttempt() {
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        when(authCache.registerFailedAttempt("camila@uceva.edu.co")).thenReturn(1L);

        assertThrows(BadCredentialsException.class, () -> authService.login("camila@uceva.edu.co", "mala"));
        verify(authCache, never()).lockAccount(anyString(), anyLong());
    }

    @Test
    void loginFailsWithSameErrorForUnknownEmail() {
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(authCache.registerFailedAttempt(anyString())).thenReturn(1L);

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> authService.login("nadie@uceva.edu.co", "x"));
        assertEquals("Credenciales incorrectas", ex.getMessage());
    }

    @Test
    void loginFailsForInactiveAccountWithSameError() {
        user.setActive(false);
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(authCache.registerFailedAttempt(anyString())).thenReturn(1L);

        assertThrows(BadCredentialsException.class, () -> authService.login("camila@uceva.edu.co", "Secreta1!"));
    }

    @Test
    void fifthFailureLocksTheAccount() {
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(authCache.registerFailedAttempt("camila@uceva.edu.co")).thenReturn(5L);

        assertThrows(AccountLockedException.class, () -> authService.login("camila@uceva.edu.co", "x"));
        verify(authCache).lockAccount("camila@uceva.edu.co", 15);
        verify(authCache).resetFailedAttempts("camila@uceva.edu.co");
    }

    @Test
    void lockedAccountIsRejectedWithoutCheckingPasswordAndRoundsMinutesUp() {
        when(authCache.isAccountLocked("camila@uceva.edu.co")).thenReturn(true);
        when(authCache.getLockSecondsLeft("camila@uceva.edu.co")).thenReturn(30L);

        AccountLockedException ex = assertThrows(AccountLockedException.class,
                () -> authService.login("camila@uceva.edu.co", "Secreta1!"));
        assertTrue(ex.getMessage().contains("1 minutos"), ex.getMessage());
        verify(userRepository, never()).findByEmailIgnoreCase(anyString());
    }

    // ------------------------------------------------------------------- logout

    @Test
    void logoutBlacklistsTokenAndDeletesRefreshToken() {
        String token = jwtService.generateAccessToken(user);

        authService.logout(token);

        ArgumentCaptor<Long> ttl = ArgumentCaptor.forClass(Long.class);
        verify(authCache).blacklistToken(eq(token), ttl.capture());
        assertTrue(ttl.getValue() > 0 && ttl.getValue() <= 900_000);
        verify(authCache).deleteRefreshToken(user.getId().toString());
    }

    @Test
    void logoutIgnoresInvalidOrBlankTokens() {
        authService.logout("basura");
        authService.logout(" ");
        authService.logout(null);

        verify(authCache, never()).blacklistToken(anyString(), anyLong());
        verify(authCache, never()).deleteRefreshToken(anyString());
    }

    // ------------------------------------------------------------------ refresh

    @Test
    void refreshRotatesTheTokenPair() {
        String refresh = jwtService.generateRefreshToken(user);
        when(authCache.getRefreshToken(user.getId().toString())).thenReturn(refresh);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        LoginResponse response = authService.refresh(refresh);

        assertEquals(user.getId(), jwtService.parseAccessToken(response.accessToken()).orElseThrow());
        verify(authCache).saveRefreshToken(user.getId().toString(), response.refreshToken(), 28_800_000);
    }

    @Test
    void refreshRejectsAccessToken() {
        String access = jwtService.generateAccessToken(user);

        assertThrows(BadCredentialsException.class, () -> authService.refresh(access));
    }

    @Test
    void refreshRejectsTokenThatIsNotTheStoredOne() {
        String refresh = jwtService.generateRefreshToken(user);
        when(authCache.getRefreshToken(user.getId().toString())).thenReturn(jwtService.generateRefreshToken(user));

        assertThrows(BadCredentialsException.class, () -> authService.refresh(refresh));
    }

    @Test
    void refreshRejectsTokenAfterLogout() {
        String refresh = jwtService.generateRefreshToken(user);
        when(authCache.getRefreshToken(user.getId().toString())).thenReturn(null);

        assertThrows(BadCredentialsException.class, () -> authService.refresh(refresh));
    }

    @Test
    void refreshRejectsDisabledAccount() {
        user.setActive(false);
        String refresh = jwtService.generateRefreshToken(user);
        when(authCache.getRefreshToken(user.getId().toString())).thenReturn(refresh);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThrows(AccountDisabledException.class, () -> authService.refresh(refresh));
    }
}
