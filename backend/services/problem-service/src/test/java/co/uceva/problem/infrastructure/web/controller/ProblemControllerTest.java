package co.uceva.problem.infrastructure.web.controller;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.uceva.problem.application.usecase.CreateProblemUseCase;
import co.uceva.problem.application.usecase.DeleteProblemUseCase;
import co.uceva.problem.application.usecase.GetAllProblemsByTitleUseCase;
import co.uceva.problem.application.usecase.GetAllProblemsUseCase;
import co.uceva.problem.application.usecase.GetProblemByIdUseCase;
import co.uceva.problem.application.usecase.UpdateProblemUseCase;
import co.uceva.problem.domain.exception.ProblemNotFoundException;
import co.uceva.problem.fixtures.ProblemFixtures;
import co.uceva.problem.infrastructure.config.JwtConfig;
import co.uceva.problem.infrastructure.config.SecurityConfig;
import co.uceva.problem.infrastructure.web.dto.CreateProblemRequestDTO;
import co.uceva.problem.infrastructure.web.dto.UpdateProblemRequestDTO;
import jakarta.servlet.ServletException;

@WebMvcTest(ProblemController.class)
@Import({SecurityConfig.class, JwtConfig.class})
@TestPropertySource(properties = {
        "app.security.jwt.secret=test-secret-key-at-least-32-bytes-long",
        "app.security.jwt.issuer=goslint-judge"
})
class ProblemControllerTest {


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateProblemUseCase createProblemUseCase;

    @MockBean
    private GetProblemByIdUseCase getProblemByIdUseCase;

    @MockBean
    private GetAllProblemsByTitleUseCase getAllProblemsByTitleUseCase;

    @MockBean
    private GetAllProblemsUseCase getAllProblemsUseCase;

    @MockBean
    private UpdateProblemUseCase updateProblemUseCase;

    @MockBean
    private DeleteProblemUseCase deleteProblemUseCase;

    private final UUID problemId = UUID.randomUUID();

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreateProblem() throws Exception {
        CreateProblemRequestDTO request = ProblemFixtures.createProblemRequest();
        when(createProblemUseCase.execute(any())).thenReturn(ProblemFixtures.aProblem(problemId, "Suma"));

        mockMvc.perform(post("/api/v1/problems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Suma"));
    }

    @Test
    void shouldGetProblemById() throws Exception {
        when(getProblemByIdUseCase.execute(problemId)).thenReturn(ProblemFixtures.aProblem(problemId, "Suma"));

        mockMvc.perform(get("/api/v1/problems/{id}", problemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Suma"));
    }

    @Test
    void shouldGetProblemsByTitle() throws Exception {
        when(getAllProblemsByTitleUseCase.execute("suma")).thenReturn(List.of(
                ProblemFixtures.aProblem(UUID.randomUUID(), "Suma A"),
                ProblemFixtures.aProblem(UUID.randomUUID(), "Suma B")
        ));

        mockMvc.perform(get("/api/v1/problems/title/{title}", "suma"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldGetAllProblems() throws Exception {
        when(getAllProblemsUseCase.execute()).thenReturn(List.of(
                ProblemFixtures.aProblem(UUID.randomUUID(), "A"),
                ProblemFixtures.aProblem(UUID.randomUUID(), "B")
        ));

        mockMvc.perform(get("/api/v1/problems/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldUpdateProblem() throws Exception {
        UpdateProblemRequestDTO request = ProblemFixtures.updateProblemRequest();
        when(updateProblemUseCase.execute(any())).thenReturn(ProblemFixtures.aProblem(problemId, "Actualizado"));

        mockMvc.perform(put("/api/v1/problems/{id}", problemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Actualizado"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldDeleteProblem() throws Exception {
        doNothing().when(deleteProblemUseCase).execute(problemId);

        mockMvc.perform(delete("/api/v1/problems/{id}", problemId))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldPropagateNotFoundException() {
        when(getProblemByIdUseCase.execute(problemId)).thenThrow(new ProblemNotFoundException(problemId));

        assertThatThrownBy(() -> mockMvc.perform(get("/api/v1/problems/{id}", problemId)))
                .isInstanceOf(ServletException.class)
                .hasCauseInstanceOf(ProblemNotFoundException.class);
    }

    @Test
    @WithMockUser(roles = "SERVICE")
    void shouldDenyServiceRoleToCreateProblem() throws Exception {
        CreateProblemRequestDTO request = ProblemFixtures.createProblemRequest();

        mockMvc.perform(post("/api/v1/problems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
