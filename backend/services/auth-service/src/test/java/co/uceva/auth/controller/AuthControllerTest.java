package co.uceva.auth.controller;

import co.uceva.auth.config.SecurityConfig;
import co.uceva.auth.dto.LoginResponse;
import co.uceva.auth.exception.AccountLockedException;
import co.uceva.auth.exception.BadCredentialsException;
import co.uceva.auth.exception.InvalidCurrentPasswordException;
import co.uceva.auth.exception.UserAlreadyExistsException;
import co.uceva.auth.exception.UsernameTakenException;
import co.uceva.auth.model.Role;
import co.uceva.auth.model.User;
import co.uceva.auth.service.AuthService;
import co.uceva.auth.service.ProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    private static final String TOKEN = "valid.test.jwt";
    private static final String BEARER = "Bearer " + TOKEN;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AuthService authService;
    @MockBean private ProfileService profileService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .firstName("Camila")
                .lastName("Restrepo")
                .username("camila_dev")
                .email("camila@uceva.edu.co")
                .role(Role.STUDENT)
                .institution("UCEVA")
                .active(true)
                .createdAt(LocalDateTime.of(2026, 3, 1, 10, 30, 0))
                .build();
    }

    private String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    // ----------------------------------------------------------------- register

    private Map<String, String> registerBody() {
        return Map.of("firstName", "Camila", "lastName", "Restrepo", "username", "camila_dev",
                "email", "camila@uceva.edu.co", "password", "Secreta1!");
    }

    @Test
    void registerReturnsCreatedWithUserId() throws Exception {
        when(authService.register(any())).thenReturn(user);

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(registerBody())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(user.getId().toString()));
    }

    @Test
    void registerReturnsConflictWhenAlreadyRegistered() throws Exception {
        when(authService.register(any())).thenThrow(new UserAlreadyExistsException("El correo electrónico ya está registrado."));

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(registerBody())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("El correo electrónico ya está registrado."));
    }

    @Test
    void registerReturnsConflictWhenDatabaseUniqueConstraintFires() throws Exception {
        when(authService.register(any())).thenThrow(new DataIntegrityViolationException("uk_users_email"));

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(registerBody())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void registerReturnsBadRequestWithFieldMessagesForWeakPassword() throws Exception {
        Map<String, String> body = new java.util.HashMap<>(registerBody());
        body.put("password", "sinmayuscula1");

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").exists());
        verify(authService, never()).register(any());
    }

    @Test
    void malformedJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // -------------------------------------------------------------------- login

    @Test
    void loginReturnsTokens() throws Exception {
        when(authService.login("camila@uceva.edu.co", "Secreta1!"))
                .thenReturn(LoginResponse.bearer("acc", "ref", 900_000));

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "camila@uceva.edu.co", "password", "Secreta1!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("acc"))
                .andExpect(jsonPath("$.refreshToken").value("ref"))
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900_000));
    }

    @Test
    void loginReturnsUnauthorizedOnBadCredentials() throws Exception {
        when(authService.login(anyString(), anyString())).thenThrow(new BadCredentialsException("Credenciales incorrectas"));

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "camila@uceva.edu.co", "password", "x"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Credenciales incorrectas"));
    }

    @Test
    void loginReturnsLockedWhenAccountIsLocked() throws Exception {
        when(authService.login(anyString(), anyString())).thenThrow(new AccountLockedException("bloqueada"));

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "camila@uceva.edu.co", "password", "x"))))
                .andExpect(status().isLocked());
    }

    // ------------------------------------------------------------------- logout

    @Test
    void logoutPassesTheTokenWithoutBearerPrefix() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer  " + TOKEN + " "))
                .andExpect(status().isOk());

        verify(authService).logout(TOKEN);
    }

    @Test
    void logoutIsOkWithoutHeaderOrWithoutBearerPrefix() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/logout").header(HttpHeaders.AUTHORIZATION, "Basic abc"))
                .andExpect(status().isOk());

        verify(authService, never()).logout(anyString());
    }

    // ------------------------------------------------------------------ refresh

    @Test
    void refreshReturnsNewTokenPair() throws Exception {
        when(authService.refresh("ref")).thenReturn(LoginResponse.bearer("acc2", "ref2", 900_000));

        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", "ref"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("acc2"))
                .andExpect(jsonPath("$.refreshToken").value("ref2"));
    }

    @Test
    void refreshReturnsUnauthorizedWhenTokenIsNotValid() throws Exception {
        when(authService.refresh("malo")).thenThrow(new BadCredentialsException("Refresh token inválido o expirado"));

        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", "malo"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshReturnsBadRequestWhenBodyHasNoToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ----------------------------------------------------------------------- me

    @Test
    void meReturnsProfileWithFieldNamesTheFrontendExpects() throws Exception {
        when(profileService.getCurrentUser(TOKEN)).thenReturn(user);

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.username").value("camila_dev"))
                .andExpect(jsonPath("$.email").value("camila@uceva.edu.co"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.createdAt").value("2026-03-01T10:30:00"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void meReturnsUnauthorizedWithoutBearerHeader() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Basic " + TOKEN))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsUnauthorizedWhenTokenIsInvalid() throws Exception {
        when(profileService.getCurrentUser("bad")).thenThrow(new BadCredentialsException("Token inválido o expirado"));

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer bad"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Token inválido o expirado"));
    }

    // --------------------------------------------------------------- /me/username

    @Test
    void updateUsernameReturnsUpdatedProfile() throws Exception {
        user.setUsername("camila_2026");
        when(profileService.updateUsername(TOKEN, "camila_2026")).thenReturn(user);

        mockMvc.perform(patch("/api/v1/auth/me/username").header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("username", "camila_2026"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("camila_2026"));
    }

    @Test
    void updateUsernameReturnsConflictWhenTaken() throws Exception {
        when(profileService.updateUsername(eq(TOKEN), anyString())).thenThrow(new UsernameTakenException("en uso"));

        mockMvc.perform(patch("/api/v1/auth/me/username").header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("username", "otro_user"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("en uso"));
    }

    @Test
    void updateUsernameReturnsBadRequestForInvalidFormat() throws Exception {
        for (String invalid : new String[] {"", "ab", "camila dev", "a".repeat(31)}) {
            mockMvc.perform(patch("/api/v1/auth/me/username").header(HttpHeaders.AUTHORIZATION, BEARER)
                            .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("username", invalid))))
                    .andExpect(status().isBadRequest());
        }
        verify(profileService, never()).updateUsername(anyString(), anyString());
    }

    @Test
    void updateUsernameReturnsUnauthorizedWithoutHeader() throws Exception {
        mockMvc.perform(patch("/api/v1/auth/me/username")
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("username", "nuevo_user"))))
                .andExpect(status().isUnauthorized());
    }

    // --------------------------------------------------------------- /me/password

    private String passwordBody(String current, String next) throws Exception {
        return json(Map.of("currentPassword", current, "newPassword", next));
    }

    @Test
    void changePasswordReturnsNoContent() throws Exception {
        mockMvc.perform(patch("/api/v1/auth/me/password").header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(passwordBody("Actual1!", "Nueva2@x")))
                .andExpect(status().isNoContent());

        verify(profileService).changePassword(TOKEN, "Actual1!", "Nueva2@x");
    }

    @Test
    void changePasswordReturnsUnprocessableWhenCurrentIsWrong() throws Exception {
        doThrow(new InvalidCurrentPasswordException("La contraseña actual es incorrecta."))
                .when(profileService).changePassword(eq(TOKEN), eq("Mala1!xx"), any());

        mockMvc.perform(patch("/api/v1/auth/me/password").header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(passwordBody("Mala1!xx", "Nueva2@x")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("La contraseña actual es incorrecta."));
    }

    @Test
    void changePasswordAppliesTheSamePolicyAsRegistration() throws Exception {
        for (String weak : new String[] {"corta1!", "sinmayuscula1!", "SinNumero!!", "SinEspecial12"}) {
            mockMvc.perform(patch("/api/v1/auth/me/password").header(HttpHeaders.AUTHORIZATION, BEARER)
                            .contentType(MediaType.APPLICATION_JSON).content(passwordBody("Actual1!", weak)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.newPassword").exists());
        }
        verify(profileService, never()).changePassword(anyString(), anyString(), anyString());
    }

    @Test
    void changePasswordReturnsBadRequestWhenCurrentIsBlank() throws Exception {
        mockMvc.perform(patch("/api/v1/auth/me/password").header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(passwordBody("", "Nueva2@x")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePasswordReturnsUnauthorizedWithoutHeader() throws Exception {
        mockMvc.perform(patch("/api/v1/auth/me/password")
                        .contentType(MediaType.APPLICATION_JSON).content(passwordBody("Actual1!", "Nueva2@x")))
                .andExpect(status().isUnauthorized());
    }
}
