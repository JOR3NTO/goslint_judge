package co.uceva.auth.controller;

import co.uceva.auth.config.SecurityConfig;
import co.uceva.auth.exception.BadCredentialsException;
import co.uceva.auth.exception.ForbiddenOperationException;
import co.uceva.auth.exception.UserNotFoundException;
import co.uceva.auth.model.Role;
import co.uceva.auth.model.User;
import co.uceva.auth.service.AdminUserService;
import co.uceva.auth.service.AuthService;
import co.uceva.auth.service.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AdminUserController.class, AuthController.class})
@Import(SecurityConfig.class)
class AdminUserControllerTest {

    private static final String TOKEN = "admin.token";
    private static final String BEARER = "Bearer " + TOKEN;

    @Autowired private MockMvc mockMvc;

    @MockBean private AdminUserService adminUserService;
    @MockBean private AuthService authService;
    @MockBean private ProfileService profileService;

    private User student;

    @BeforeEach
    void setUp() {
        student = User.builder()
                .id(UUID.randomUUID()).firstName("Camila").lastName("Restrepo")
                .username("camila_dev").email("camila@uceva.edu.co").passwordHash("secreto")
                .role(Role.STUDENT).institution("UCEVA").active(true)
                .createdAt(LocalDateTime.of(2026, 3, 1, 10, 30, 0))
                .build();
    }

    // ---------------------------------------------------------------- listado

    @Test
    void listReturnsPageWithTotals() throws Exception {
        when(adminUserService.listUsers(TOKEN, "cami", 1, 5))
                .thenReturn(new PageImpl<>(List.of(student), PageRequest.of(1, 5), 11));

        mockMvc.perform(get("/api/v1/admin/users").param("page", "1").param("size", "5").param("search", "cami")
                        .header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("camila_dev"))
                .andExpect(jsonPath("$.content[0].role").value("STUDENT"))
                .andExpect(jsonPath("$.content[0].isActive").value(true))
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.totalElements").value(11));
    }

    @Test
    void listUsesDefaultPageAndSize() throws Exception {
        when(adminUserService.listUsers(eq(TOKEN), isNull(), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/admin/users").header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isOk());

        verify(adminUserService).listUsers(TOKEN, null, 0, 20);
    }

    @Test
    void listWithoutBearerIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")).andExpect(status().isUnauthorized());
        verify(adminUserService, never()).listUsers(eq(TOKEN), isNull(), anyInt(), anyInt());
    }

    @Test
    void listWithInvalidTokenIsUnauthorized() throws Exception {
        when(adminUserService.listUsers(eq("bad"), isNull(), anyInt(), anyInt()))
                .thenThrow(new BadCredentialsException("Token inválido o expirado"));

        mockMvc.perform(get("/api/v1/admin/users").header(HttpHeaders.AUTHORIZATION, "Bearer bad"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listByNonAdminIsForbidden() throws Exception {
        when(adminUserService.listUsers(eq(TOKEN), isNull(), anyInt(), anyInt()))
                .thenThrow(new ForbiddenOperationException("Se requiere rol de administrador."));

        mockMvc.perform(get("/api/v1/admin/users").header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Se requiere rol de administrador."));
    }

    // ------------------------------------------------------------------- rol

    @Test
    void changeRoleReturnsUpdatedUser() throws Exception {
        student.setRole(Role.ORGANIZER);
        when(adminUserService.changeRole(TOKEN, student.getId(), Role.ORGANIZER)).thenReturn(student);

        mockMvc.perform(patch("/api/v1/admin/users/{id}/role", student.getId())
                        .header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ORGANIZER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ORGANIZER"));
    }

    @Test
    void changeRoleRejectsUnknownOrMissingRole() throws Exception {
        for (String body : new String[] {"{\"role\":\"EMPERADOR\"}", "{}", "{\"role\":null}"}) {
            mockMvc.perform(patch("/api/v1/admin/users/{id}/role", student.getId())
                            .header(HttpHeaders.AUTHORIZATION, BEARER)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void changeRoleBreakingAHierarchyRuleIsForbidden() throws Exception {
        when(adminUserService.changeRole(TOKEN, student.getId(), Role.ADMIN))
                .thenThrow(new ForbiddenOperationException("Solo el super administrador puede asignar el rol de administrador."));

        mockMvc.perform(patch("/api/v1/admin/users/{id}/role", student.getId())
                        .header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void changeRoleOfUnknownUserIsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(adminUserService.changeRole(TOKEN, id, Role.ORGANIZER)).thenThrow(new UserNotFoundException("Usuario no encontrado."));

        mockMvc.perform(patch("/api/v1/admin/users/{id}/role", id)
                        .header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ORGANIZER\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void malformedUserIdIsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/no-es-uuid/role")
                        .header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ORGANIZER\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeRoleWithoutBearerIsUnauthorized() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/{id}/role", student.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ORGANIZER\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- estado

    @Test
    void changeStatusReturnsUpdatedUser() throws Exception {
        student.setActive(false);
        when(adminUserService.changeStatus(TOKEN, student.getId(), false)).thenReturn(student);

        mockMvc.perform(patch("/api/v1/admin/users/{id}/status", student.getId())
                        .header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"isActive\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    void selfDeactivationIsForbidden() throws Exception {
        when(adminUserService.changeStatus(TOKEN, student.getId(), false))
                .thenThrow(new ForbiddenOperationException("No puedes desactivar tu propia cuenta."));

        mockMvc.perform(patch("/api/v1/admin/users/{id}/status", student.getId())
                        .header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"isActive\":false}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No puedes desactivar tu propia cuenta."));
    }

    @Test
    void changeStatusRequiresTheFlag() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/{id}/status", student.getId())
                        .header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    // --------------------------------------------------------- check-username

    @Test
    void checkUsernameIsPublicAndReportsAvailability() throws Exception {
        when(profileService.isUsernameAvailable("libre_01", null)).thenReturn(true);
        when(profileService.isUsernameAvailable("ocupado", null)).thenReturn(false);

        mockMvc.perform(get("/api/v1/auth/check-username").param("username", "libre_01"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.available").value(true));
        mockMvc.perform(get("/api/v1/auth/check-username").param("username", "ocupado"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void checkUsernamePassesTheOptionalToken() throws Exception {
        when(profileService.isUsernameAvailable("camila_dev", TOKEN)).thenReturn(true);

        mockMvc.perform(get("/api/v1/auth/check-username").param("username", "camila_dev")
                        .header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isOk()).andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void checkUsernameRejectsInvalidFormatAndMissingParam() throws Exception {
        mockMvc.perform(get("/api/v1/auth/check-username").param("username", "a b"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/auth/check-username").param("username", "ab"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/auth/check-username"))
                .andExpect(status().isBadRequest());
    }
}
