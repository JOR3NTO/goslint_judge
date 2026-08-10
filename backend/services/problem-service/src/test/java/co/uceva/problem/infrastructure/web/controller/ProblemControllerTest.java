package co.uceva.problem.infrastructure.web.controller;

import co.uceva.problem.application.usecase.*;
import co.uceva.problem.domain.exception.ProblemNotFoundException;
import co.uceva.problem.domain.model.Problem;
import co.uceva.problem.fixtures.ProblemFixtures;
import co.uceva.problem.infrastructure.config.SecurityConfig;
import co.uceva.problem.infrastructure.web.dto.CreateProblemRequestDTO;
import co.uceva.problem.infrastructure.web.dto.UpdateProblemRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.servlet.ServletException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProblemController.class)
@Import(SecurityConfig.class)
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
}
