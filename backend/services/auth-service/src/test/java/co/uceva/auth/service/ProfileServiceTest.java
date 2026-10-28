package co.uceva.auth.service;

import co.uceva.auth.cache.AuthCache;
import co.uceva.auth.exception.AccountDisabledException;
import co.uceva.auth.exception.BadCredentialsException;
import co.uceva.auth.exception.InvalidCurrentPasswordException;
import co.uceva.auth.exception.InvalidUserDataException;
import co.uceva.auth.exception.UsernameTakenException;
import co.uceva.auth.model.Role;
import co.uceva.auth.model.User;
import co.uceva.auth.repository.UserRepository;
import co.uceva.auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthCache authCache;

    private final JwtService jwtService = new JwtService(SECRET, "test", 900_000, 28_800_000);
    private ProfileService profileService;
    private User user;
    private String token;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(userRepository, passwordEncoder, authCache, jwtService);
        user = User.builder()
                .id(UUID.randomUUID())
                .username("camila")
                .email("camila@uceva.edu.co")
                .passwordHash("hash")
                .role(Role.STUDENT)
                .active(true)
                .build();
        token = jwtService.generateAccessToken(user);
    }

    private void tokenBelongsToExistingUser() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    }

    // ------------------------------------------------------------ getCurrentUser

    @Test
    void getCurrentUserReturnsTheTokenOwner() {
        tokenBelongsToExistingUser();

        assertSame(user, profileService.getCurrentUser(token));
    }

    @Test
    void getCurrentUserRejectsMissingToken() {
        assertThrows(BadCredentialsException.class, () -> profileService.getCurrentUser(null));
        assertThrows(BadCredentialsException.class, () -> profileService.getCurrentUser(" "));
    }

    @Test
    void getCurrentUserRejectsRevokedToken() {
        when(authCache.isBlacklisted(token)).thenReturn(true);

        assertThrows(BadCredentialsException.class, () -> profileService.getCurrentUser(token));
    }

    @Test
    void getCurrentUserRejectsInvalidToken() {
        assertThrows(BadCredentialsException.class, () -> profileService.getCurrentUser("basura"));
    }

    @Test
    void getCurrentUserRejectsRefreshToken() {
        String refresh = jwtService.generateRefreshToken(user);

        assertThrows(BadCredentialsException.class, () -> profileService.getCurrentUser(refresh));
    }

    @Test
    void getCurrentUserRejectsUnknownUser() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> profileService.getCurrentUser(token));
    }

    @Test
    void getCurrentUserRejectsDisabledAccount() {
        user.setActive(false);
        tokenBelongsToExistingUser();

        assertThrows(AccountDisabledException.class, () -> profileService.getCurrentUser(token));
    }

    // ------------------------------------------------------------ updateUsername

    @Test
    void updateUsernamePersistsTheNewOne() {
        tokenBelongsToExistingUser();
        when(userRepository.existsByUsernameIgnoreCaseAndIdNot("nuevo_user", user.getId())).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);

        User updated = profileService.updateUsername(token, " nuevo_user ");

        assertEquals("nuevo_user", updated.getUsername());
    }

    @Test
    void updateUsernameWithSameValueDoesNotTouchTheDatabase() {
        tokenBelongsToExistingUser();

        assertSame(user, profileService.updateUsername(token, "camila"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUsernameAllowsChangingOnlyTheCapitalization() {
        tokenBelongsToExistingUser();
        when(userRepository.existsByUsernameIgnoreCaseAndIdNot("Camila", user.getId())).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);

        assertEquals("Camila", profileService.updateUsername(token, "Camila").getUsername());
    }

    @Test
    void updateUsernameRejectsOneUsedByAnotherUser() {
        tokenBelongsToExistingUser();
        when(userRepository.existsByUsernameIgnoreCaseAndIdNot("ocupado", user.getId())).thenReturn(true);

        assertThrows(UsernameTakenException.class, () -> profileService.updateUsername(token, "ocupado"));
        verify(userRepository, never()).save(any());
    }

    // ------------------------------------------------------------ changePassword

    @Test
    void changePasswordSavesNewHashAndClosesAllSessions() {
        tokenBelongsToExistingUser();
        when(passwordEncoder.matches("Actual1!", "hash")).thenReturn(true);
        when(passwordEncoder.matches("Nueva2@x", "hash")).thenReturn(false);
        when(passwordEncoder.encode("Nueva2@x")).thenReturn("nuevo-hash");

        profileService.changePassword(token, "Actual1!", "Nueva2@x");

        assertEquals("nuevo-hash", user.getPasswordHash());
        verify(userRepository).save(user);
        verify(authCache).blacklistToken(eq(token), anyLong());
        verify(authCache).deleteRefreshToken(user.getId().toString());
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() {
        tokenBelongsToExistingUser();
        when(passwordEncoder.matches("mala", "hash")).thenReturn(false);

        assertThrows(InvalidCurrentPasswordException.class,
                () -> profileService.changePassword(token, "mala", "Nueva2@x"));
        verify(userRepository, never()).save(any());
        verify(authCache, never()).deleteRefreshToken(anyString());
    }

    @Test
    void changePasswordRejectsReusingTheCurrentPassword() {
        tokenBelongsToExistingUser();
        when(passwordEncoder.matches("Actual1!", "hash")).thenReturn(true);

        assertThrows(InvalidUserDataException.class,
                () -> profileService.changePassword(token, "Actual1!", "Actual1!"));
        verify(userRepository, never()).save(any());
    }

    // -------------------------------------------------------- isUsernameAvailable

    @Test
    void usernameIsAvailableWhenNobodyHasIt() {
        when(userRepository.existsByUsernameIgnoreCase("libre")).thenReturn(false);

        org.junit.jupiter.api.Assertions.assertTrue(profileService.isUsernameAvailable(" libre ", null));
    }

    @Test
    void usernameIsNotAvailableWhenTaken() {
        when(userRepository.existsByUsernameIgnoreCase("ocupado")).thenReturn(true);

        org.junit.jupiter.api.Assertions.assertFalse(profileService.isUsernameAvailable("ocupado", null));
    }

    @Test
    void ownUsernameCountsAsAvailableForTheLoggedInUser() {
        tokenBelongsToExistingUser();

        org.junit.jupiter.api.Assertions.assertTrue(profileService.isUsernameAvailable("CAMILA", token));
        verify(userRepository, never()).existsByUsernameIgnoreCase(anyString());
    }

    @Test
    void invalidTokenIsIgnoredAndAnsweredLikeAVisitor() {
        when(userRepository.existsByUsernameIgnoreCase("camila")).thenReturn(true);

        org.junit.jupiter.api.Assertions.assertFalse(profileService.isUsernameAvailable("camila", "basura"));
    }

    @Test
    void changePasswordRejectsInvalidToken() {
        assertThrows(BadCredentialsException.class,
                () -> profileService.changePassword("basura", "Actual1!", "Nueva2@x"));
    }
}
