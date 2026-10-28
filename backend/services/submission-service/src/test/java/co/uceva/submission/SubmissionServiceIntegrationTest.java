package co.uceva.submission;

import co.uceva.submission.fixtures.SubmissionFixtures;
import co.uceva.submission.infrastructure.web.dto.SubmitCodeRequestDTO;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import co.uceva.submission.application.port.out.ContestDataPort;
import co.uceva.submission.domain.model.ContestSnapshot;
import co.uceva.submission.domain.model.TeamSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.when;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.uceva.shared.domain.ProgrammingLanguage;

@SpringBootTest
@AutoConfigureMockMvc
class SubmissionServiceIntegrationTest extends AbstractIntegrationTest {

    /** Se simula contest-service para que el envío sea elegible sin salir a la red. */
    @MockBean
    private ContestDataPort contestDataPort;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldExecuteFullSubmissionLifecycle() throws Exception {
        UUID teamId = UUID.randomUUID();
        UUID problemId = UUID.randomUUID();

        // 0. El estudiante es integrante de un equipo de un concurso activo que incluye el problema
        UUID contestId = UUID.randomUUID();
        when(contestDataPort.findTeam(teamId)).thenReturn(Optional.of(
                new TeamSnapshot(teamId, contestId, List.of(SubmissionFixtures.USER_ID))));
        when(contestDataPort.findContest(contestId)).thenReturn(Optional.of(new ContestSnapshot(
                contestId, Instant.now().minusSeconds(3600), Instant.now().plusSeconds(3600))));
        when(contestDataPort.findContestProblemIds(contestId)).thenReturn(List.of(problemId));

        // 1. Create submission
        SubmitCodeRequestDTO request = new SubmitCodeRequestDTO(
                teamId, problemId, ProgrammingLanguage.PYTHON, "print(1)"
        );
        MvcResult createResult = mockMvc.perform(post("/api/v1/submissions")
                        .with(authentication(SubmissionFixtures.authenticatedAs("STUDENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID submissionId = extractId(createResult);

        // 2. Get by id
        mockMvc.perform(get("/api/v1/submissions/{id}", submissionId)
                        .with(user("student").roles("STUDENT")))
                .andExpect(status().isOk());

        // 3. Get all (ADMIN)
        mockMvc.perform(get("/api/v1/submissions")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());

        // 4. Get by problem (ADMIN)
        mockMvc.perform(get("/api/v1/submissions/problem/{problemId}", problemId)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());

        // 5. Get by team
        mockMvc.perform(get("/api/v1/submissions/team/{teamId}", teamId)
                        .with(user("student").roles("STUDENT")))
                .andExpect(status().isOk());

        // 6. Get history
        mockMvc.perform(get("/api/v1/submissions/history")
                        .param("problemId", problemId.toString())
                        .param("teamId", teamId.toString())
                        .with(user("student").roles("STUDENT")))
                .andExpect(status().isOk());

        // 7. Get metrics
        mockMvc.perform(get("/api/v1/submissions/{id}/metrics", submissionId)
                        .with(user("student").roles("STUDENT")))
                .andExpect(status().isOk());

        // 8. Delete
        mockMvc.perform(delete("/api/v1/submissions/{id}", submissionId)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());
    }

    private UUID extractId(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(json.get("id").asText());
    }
}
