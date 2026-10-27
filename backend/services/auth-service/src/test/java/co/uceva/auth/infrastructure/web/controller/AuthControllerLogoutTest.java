package co.uceva.auth.infrastructure.web.controller;

import co.uceva.auth.application.usecase.LoginUserUseCase;
import co.uceva.auth.application.usecase.LogoutUserUseCase;
import co.uceva.auth.application.usecase.RegisterUserUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integracion del endpoint POST /api/v1/auth/logout (HU-03).
 *
 * Usa WebMvcTest para levantar solo la capa web del controlador,
 * mockeando los casos de uso de dominio.
 *
 * Criterios de aceptacion verificados (HU-03):
 * CA-1: El controlador delega la invalidacion al use case cuando llega un token Bearer.
 * CA-1: La respuesta es siempre 200 OK, incluso sin token.
 * CA-2: El rawToken que llega al use case no contiene el prefijo "Bearer ".
 */
import co.uceva.auth.infrastructure.config.SecurityConfig;
import org.springframework.context.annotation.Import;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerLogoutTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LogoutUserUseCase logoutUserUseCase;

    @MockBean
    private LoginUserUseCase loginUserUseCase;

    @MockBean
    private RegisterUserUseCase registerUserUseCase;

    @Test
    @WithAnonymousUser
    void returnsOkAndDelegatesToUseCaseWhenValidBearerTokenIsProvided() throws Exception {
        String rawToken = "eyJhbGciOiJIUzI1NiJ9.test.signature";

        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + rawToken))
                .andExpect(status().isOk());

        verify(logoutUserUseCase).logout(rawToken);
    }

    @Test
    @WithAnonymousUser
    void returnsOkWithoutCallingUseCaseWhenNoAuthorizationHeaderIsProvided() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(logoutUserUseCase, never()).logout(anyString());
    }

    @Test
    @WithAnonymousUser
    void returnsOkWithoutCallingUseCaseWhenAuthorizationHeaderLacksBearerPrefix() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz"))
                .andExpect(status().isOk());

        verify(logoutUserUseCase, never()).logout(anyString());
    }

    @Test
    @WithAnonymousUser
    void returnsOkAndDelegatesToUseCaseWhenBearerTokenIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer   "))
                .andExpect(status().isOk());

        verify(logoutUserUseCase).logout("");
    }
}