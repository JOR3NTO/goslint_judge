package co.uceva.submission.infrastructure.web.controller;

import co.uceva.submission.application.usecase.DeleteSubmissionUseCase;
import co.uceva.submission.application.usecase.GetAllSubmissionsUseCase;
import co.uceva.submission.application.usecase.GetSubmissionByIdUseCase;
import co.uceva.submission.application.usecase.GetSubmissionHistoryUseCase;
import co.uceva.submission.application.usecase.GetSubmissionMetricsUseCase;
import co.uceva.submission.application.usecase.GetSubmissionMetricsUseCase.SubmissionMetrics;
import co.uceva.submission.application.usecase.GetSubmissionsByProblemUseCase;
import co.uceva.submission.application.usecase.GetSubmissionsByTeamUseCase;
import co.uceva.submission.application.usecase.SubmitCodeUseCase;
import co.uceva.submission.domain.exception.DuplicateSubmissionException;
import co.uceva.submission.domain.exception.SubmissionNotFoundException;
import co.uceva.submission.domain.model.Submission;
import co.uceva.submission.fixtures.SubmissionFixtures;
import co.uceva.submission.infrastructure.config.SecurityConfig;
import co.uceva.submission.infrastructure.web.dto.SubmitCodeRequestDTO;
import co.uceva.submission.infrastructure.web.exception.SubmissionExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubmissionController.class)
@Import({SecurityConfig.class, SubmissionExceptionHandler.class})
class SubmissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SubmitCodeUseCase submitCodeUseCase;

    @MockBean
    private GetSubmissionByIdUseCase getSubmissionByIdUseCase;

    @MockBean
    private GetAllSubmissionsUseCase getAllSubmissionsUseCase;

    @MockBean
    private GetSubmissionsByProblemUseCase getSubmissionsByProblemUseCase;

    @MockBean
    private GetSubmissionsByTeamUseCase getSubmissionsByTeamUseCase;

    @MockBean
    private GetSubmissionHistoryUseCase getSubmissionHistoryUseCase;

    @MockBean
    private GetSubmissionMetricsUseCase getSubmissionMetricsUseCase;

    @MockBean
    private DeleteSubmissionUseCase deleteSubmissionUseCase;

    private final UUID submissionId = UUID.randomUUID();
    private final UUID problemId = UUID.randomUUID();
    private final UUID teamId = UUID.randomUUID();

    @Test
    @WithMockUser(roles = "STUDENT")
    void shouldCreateSubmission() throws Exception {
        SubmitCodeRequestDTO request = SubmissionFixtures.submitCodeRequest();
        when(submitCodeUseCase.execute(any())).thenReturn(SubmissionFixtures.aSubmission(submissionId));

        mockMvc.perform(post("/api/v1/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(submissionId.toString()))
                .andExpect(jsonPath("$.verdict").value("PENDING"));
    }

    @Test
    @WithMockUser(roles = "SERVICE")
    void shouldDenyServiceRoleToCreateSubmission() throws Exception {
        SubmitCodeRequestDTO request = SubmissionFixtures.submitCodeRequest();

        mockMvc.perform(post("/api/v1/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void shouldGetSubmissionById() throws Exception {
        when(getSubmissionByIdUseCase.execute(submissionId)).thenReturn(SubmissionFixtures.aSubmission(submissionId));

        mockMvc.perform(get("/api/v1/submissions/{id}", submissionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(submissionId.toString()));
    }

    @Test
    @WithMockUser(roles = "GUEST")
    void shouldDenyGuestRoleToGetSubmissionById() throws Exception {
        mockMvc.perform(get("/api/v1/submissions/{id}", submissionId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void shouldReturnNotFoundWhenSubmissionDoesNotExist() throws Exception {
        when(getSubmissionByIdUseCase.execute(submissionId)).thenThrow(new SubmissionNotFoundException(submissionId));

        mockMvc.perform(get("/api/v1/submissions/{id}", submissionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void shouldReturnConflictWhenSubmissionIsDuplicate() throws Exception {
        SubmitCodeRequestDTO request = SubmissionFixtures.submitCodeRequest();
        when(submitCodeUseCase.execute(any())).thenThrow(
                new DuplicateSubmissionException(teamId, problemId, co.uceva.shared.domain.ProgrammingLanguage.PYTHON));

        mockMvc.perform(post("/api/v1/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetAllSubmissions() throws Exception {
        when(getAllSubmissionsUseCase.execute()).thenReturn(List.of(
                SubmissionFixtures.aSubmission(UUID.randomUUID()),
                SubmissionFixtures.aSubmission(UUID.randomUUID())
        ));

        mockMvc.perform(get("/api/v1/submissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void shouldDenyStudentRoleToGetAllSubmissions() throws Exception {
        mockMvc.perform(get("/api/v1/submissions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetSubmissionsByProblem() throws Exception {
        when(getSubmissionsByProblemUseCase.execute(problemId)).thenReturn(List.of(
                SubmissionFixtures.aSubmission(UUID.randomUUID())
        ));

        mockMvc.perform(get("/api/v1/submissions/problem/{problemId}", problemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void shouldDenyStudentRoleToGetSubmissionsByProblem() throws Exception {
        mockMvc.perform(get("/api/v1/submissions/problem/{problemId}", problemId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void shouldGetSubmissionsByTeam() throws Exception {
        when(getSubmissionsByTeamUseCase.execute(teamId)).thenReturn(List.of(
                SubmissionFixtures.aSubmission(UUID.randomUUID())
        ));

        mockMvc.perform(get("/api/v1/submissions/team/{teamId}", teamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(roles = "SERVICE")
    void shouldDenyServiceRoleToGetSubmissionsByTeam() throws Exception {
        mockMvc.perform(get("/api/v1/submissions/team/{teamId}", teamId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void shouldGetSubmissionHistory() throws Exception {
        when(getSubmissionHistoryUseCase.execute(any())).thenReturn(List.of(
                SubmissionFixtures.aSubmission(UUID.randomUUID())
        ));

        mockMvc.perform(get("/api/v1/submissions/history")
                        .param("problemId", problemId.toString())
                        .param("teamId", teamId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(roles = "SERVICE")
    void shouldDenyServiceRoleToGetSubmissionHistory() throws Exception {
        mockMvc.perform(get("/api/v1/submissions/history")
                        .param("problemId", problemId.toString())
                        .param("teamId", teamId.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void shouldGetMetrics() throws Exception {
        when(getSubmissionMetricsUseCase.execute(submissionId)).thenReturn(
                new SubmissionMetrics(submissionId, co.uceva.shared.domain.VerdictStatus.ACCEPTED, 100, 2048, 512)
        );

        mockMvc.perform(get("/api/v1/submissions/{id}/metrics", submissionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submissionId").value(submissionId.toString()))
                .andExpect(jsonPath("$.verdict").value("ACCEPTED"));
    }

    @Test
    @WithMockUser(roles = "GUEST")
    void shouldDenyGuestRoleToGetMetrics() throws Exception {
        mockMvc.perform(get("/api/v1/submissions/{id}/metrics", submissionId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldDeleteSubmission() throws Exception {
        doNothing().when(deleteSubmissionUseCase).execute(submissionId);

        mockMvc.perform(delete("/api/v1/submissions/{id}", submissionId))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void shouldDenyStudentRoleToDeleteSubmission() throws Exception {
        mockMvc.perform(delete("/api/v1/submissions/{id}", submissionId))
                .andExpect(status().isForbidden());
    }
}
