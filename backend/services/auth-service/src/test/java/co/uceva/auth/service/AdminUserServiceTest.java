package co.uceva.auth.service;

import co.uceva.auth.cache.AuthCache;
import co.uceva.auth.exception.BadCredentialsException;
import co.uceva.auth.exception.ForbiddenOperationException;
import co.uceva.auth.exception.UserNotFoundException;
import co.uceva.auth.model.Role;
import co.uceva.auth.model.User;
import co.uceva.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    private static final String TOKEN = "admin.token";

    @Mock private UserRepository userRepository;
    @Mock private ProfileService profileService;
    @Mock private AuthCache authCache;

    private AdminUserService service;

    @BeforeEach
    void setUp() {
        service = new AdminUserService(userRepository, profileService, authCache);
    }

    private User user(Role role) {
        return User.builder().id(UUID.randomUUID()).username("u" + role).email(role + "@x.co")
                .role(role).active(true).build();
    }

    /** El token pertenece a {@code actor}. */
    private User actingAs(Role role) {
        User actor = user(role);
        when(profileService.getCurrentUser(TOKEN)).thenReturn(actor);
        return actor;
    }

    private User targetWith(Role role) {
        User target = user(role);
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        lenientSave();
        return target;
    }

    private void lenientSave() {
        org.mockito.Mockito.lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------------------------------------------------------------- acceso

    @Test
    void nonAdminsAreRejectedOnEveryOperation() {
        for (Role role : new Role[] {Role.STUDENT, Role.ORGANIZER}) {
            User actor = user(role);
            when(profileService.getCurrentUser(TOKEN)).thenReturn(actor);

            assertThrows(ForbiddenOperationException.class, () -> service.listUsers(TOKEN, null, 0, 20));
            assertThrows(ForbiddenOperationException.class, () -> service.changeRole(TOKEN, UUID.randomUUID(), Role.ORGANIZER));
            assertThrows(ForbiddenOperationException.class, () -> service.changeStatus(TOKEN, UUID.randomUUID(), false));
        }
        verify(userRepository, never()).save(any());
    }

    @Test
    void invalidTokenPropagatesAsBadCredentials() {
        when(profileService.getCurrentUser(TOKEN)).thenThrow(new BadCredentialsException("Token inválido o expirado"));

        assertThrows(BadCredentialsException.class, () -> service.listUsers(TOKEN, null, 0, 20));
    }

    // ---------------------------------------------------------------- listado

    @Test
    void listWithoutSearchReturnsEveryoneNewestFirst() {
        actingAs(Role.ADMIN);
        Page<User> page = new PageImpl<>(List.of(user(Role.STUDENT)));
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(userRepository.findAll(pageable.capture())).thenReturn(page);

        assertSame(page, service.listUsers(TOKEN, "  ", 2, 10));
        assertEquals(2, pageable.getValue().getPageNumber());
        assertEquals(10, pageable.getValue().getPageSize());
        assertEquals("createdAt: DESC", pageable.getValue().getSort().toString());
    }

    @Test
    void listWithSearchFiltersByUsernameOrEmailUsingTrimmedTerm() {
        actingAs(Role.SUPER_ADMIN);
        Page<User> page = new PageImpl<>(List.of());
        when(userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                org.mockito.ArgumentMatchers.eq("cami"), org.mockito.ArgumentMatchers.eq("cami"), any(Pageable.class)))
                .thenReturn(page);

        assertSame(page, service.listUsers(TOKEN, " cami ", 0, 20));
    }

    @Test
    void listClampsPageAndSize() {
        actingAs(Role.ADMIN);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(userRepository.findAll(pageable.capture())).thenReturn(new PageImpl<>(List.of()));

        service.listUsers(TOKEN, null, -5, 100_000);
        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(100, pageable.getValue().getPageSize());

        service.listUsers(TOKEN, null, 0, 0);
        assertEquals(1, pageable.getValue().getPageSize());
    }

    // ------------------------------------------------------------------- rol

    @Test
    void adminSwitchesStudentToOrganizerAndClosesTheirSession() {
        actingAs(Role.ADMIN);
        User target = targetWith(Role.STUDENT);

        User result = service.changeRole(TOKEN, target.getId(), Role.ORGANIZER);

        assertEquals(Role.ORGANIZER, result.getRole());
        verify(userRepository).save(target);
        verify(authCache).deleteRefreshToken(target.getId().toString());
    }

    @Test
    void adminCannotGrantAdminRole() {
        actingAs(Role.ADMIN);
        User target = targetWith(Role.STUDENT);

        assertThrows(ForbiddenOperationException.class, () -> service.changeRole(TOKEN, target.getId(), Role.ADMIN));
        assertEquals(Role.STUDENT, target.getRole());
        verify(userRepository, never()).save(any());
    }

    @Test
    void adminCannotTouchAnotherAdmin() {
        actingAs(Role.ADMIN);
        User otherAdmin = targetWith(Role.ADMIN);

        assertThrows(ForbiddenOperationException.class, () -> service.changeRole(TOKEN, otherAdmin.getId(), Role.STUDENT));
        assertThrows(ForbiddenOperationException.class, () -> service.changeStatus(TOKEN, otherAdmin.getId(), false));
        verify(userRepository, never()).save(any());
    }

    @Test
    void superAdminGrantsAndRevokesAdminRole() {
        actingAs(Role.SUPER_ADMIN);
        User student = targetWith(Role.STUDENT);
        User admin = targetWith(Role.ADMIN);

        assertEquals(Role.ADMIN, service.changeRole(TOKEN, student.getId(), Role.ADMIN).getRole());
        assertEquals(Role.STUDENT, service.changeRole(TOKEN, admin.getId(), Role.STUDENT).getRole());
    }

    @Test
    void nobodyCanAssignSuperAdminRole() {
        for (Role role : new Role[] {Role.ADMIN, Role.SUPER_ADMIN}) {
            when(profileService.getCurrentUser(TOKEN)).thenReturn(user(role));
            User target = targetWith(Role.STUDENT);

            assertThrows(ForbiddenOperationException.class,
                    () -> service.changeRole(TOKEN, target.getId(), Role.SUPER_ADMIN));
        }
        verify(userRepository, never()).save(any());
    }

    @Test
    void nobodyCanModifyTheSuperAdminAccount() {
        for (Role role : new Role[] {Role.ADMIN, Role.SUPER_ADMIN}) {
            User actor = user(role);
            when(profileService.getCurrentUser(TOKEN)).thenReturn(actor);
            User superAdmin = targetWith(Role.SUPER_ADMIN);

            assertThrows(ForbiddenOperationException.class, () -> service.changeRole(TOKEN, superAdmin.getId(), Role.STUDENT));
            assertThrows(ForbiddenOperationException.class, () -> service.changeStatus(TOKEN, superAdmin.getId(), false));
        }
        verify(userRepository, never()).save(any());
    }

    @Test
    void nobodyCanChangeTheirOwnRole() {
        for (Role role : new Role[] {Role.ADMIN, Role.SUPER_ADMIN}) {
            User actor = user(role);
            when(profileService.getCurrentUser(TOKEN)).thenReturn(actor);
            when(userRepository.findById(actor.getId())).thenReturn(Optional.of(actor));

            assertThrows(ForbiddenOperationException.class, () -> service.changeRole(TOKEN, actor.getId(), Role.STUDENT));
            assertEquals(role, actor.getRole());
        }
        verify(userRepository, never()).save(any());
    }

    @Test
    void settingTheSameRoleDoesNothing() {
        actingAs(Role.ADMIN);
        User target = targetWith(Role.ORGANIZER);

        assertSame(target, service.changeRole(TOKEN, target.getId(), Role.ORGANIZER));
        verify(userRepository, never()).save(any());
        verify(authCache, never()).deleteRefreshToken(anyString());
    }

    @Test
    void unknownTargetIsNotFound() {
        actingAs(Role.ADMIN);
        UUID missing = UUID.randomUUID();
        when(userRepository.findById(missing)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.changeRole(TOKEN, missing, Role.ORGANIZER));
        assertThrows(UserNotFoundException.class, () -> service.changeStatus(TOKEN, missing, false));
    }

    // ---------------------------------------------------------------- estado

    @Test
    void deactivatingAccountClosesItsSession() {
        actingAs(Role.ADMIN);
        User target = targetWith(Role.STUDENT);

        User result = service.changeStatus(TOKEN, target.getId(), false);

        assertFalse(result.isActive());
        verify(authCache).deleteRefreshToken(target.getId().toString());
    }

    @Test
    void reactivatingAccountDoesNotTouchSessions() {
        actingAs(Role.ADMIN);
        User target = targetWith(Role.STUDENT);
        target.setActive(false);

        assertEquals(true, service.changeStatus(TOKEN, target.getId(), true).isActive());
        verify(authCache, never()).deleteRefreshToken(anyString());
    }

    @Test
    void adminCannotDeactivateThemself() {
        for (Role role : new Role[] {Role.ADMIN, Role.SUPER_ADMIN}) {
            User actor = user(role);
            when(profileService.getCurrentUser(TOKEN)).thenReturn(actor);
            when(userRepository.findById(actor.getId())).thenReturn(Optional.of(actor));

            assertThrows(ForbiddenOperationException.class, () -> service.changeStatus(TOKEN, actor.getId(), false));
            assertEquals(true, actor.isActive());
        }
        verify(userRepository, never()).save(any());
    }

    @Test
    void superAdminDeactivatesAnAdmin() {
        actingAs(Role.SUPER_ADMIN);
        User admin = targetWith(Role.ADMIN);

        assertFalse(service.changeStatus(TOKEN, admin.getId(), false).isActive());
    }
}
