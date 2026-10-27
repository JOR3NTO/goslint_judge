package co.uceva.auth.infrastructure.web.controller;

import co.uceva.auth.application.usecase.GetCurrentUserUseCase;
import co.uceva.auth.application.usecase.LoginUserUseCase;
import co.uceva.auth.application.usecase.LogoutUserUseCase;
import co.uceva.auth.application.usecase.RegisterUserUseCase;
import co.uceva.auth.domain.exception.BadCredentialsException;
import co.uceva.auth.domain.model.Role;
import co.uceva.auth.domain.model.User;
import co.uceva.auth.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerMeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetCurrentUserUseCase getCurrentUserUseCase;

    @MockBean
    private LogoutUserUseCase logoutUserUseCase;

    @MockBean
    private LoginUserUseCase loginUserUseCase;

    @MockBean
    private RegisterUserUseCase registerUserUseCase;

    @Test
    @WithAnonymousUser
    void returnsOkWithUserProfileWhenValidTokenProvided() throws Exception {
        String token = "valid.test.jwt";
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .firstName("Camila")
                .lastName("Restrepo")
                .username("camila_dev")
                .email("camila@uceva.edu.co")
                .role(Role.STUDENT)
                .institution("UCEVA")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(getCurrentUserUseCase.getCurrentUser(token)).thenReturn(user);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("camila_dev"))
                .andExpect(jsonPath("$.email").value("camila@uceva.edu.co"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    @WithAnonymousUser
    void returnsUnauthorizedWhenHeaderMissing() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithAnonymousUser
    void returnsUnauthorizedWhenHeaderLacksBearerPrefix() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Basic token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithAnonymousUser
    void returnsUnauthorizedWhenUseCaseThrowsBadCredentials() throws Exception {
        String token = "invalid.token";
        when(getCurrentUserUseCase.getCurrentUser(token))
                .thenThrow(new BadCredentialsException("Token inválido o expirado"));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Token inválido o expirado"));
    }
}
