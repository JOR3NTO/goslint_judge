package co.uceva.submission;

import co.uceva.submission.infrastructure.web.dto.SubmitCodeRequestDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SubmissionServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldExecuteFullSubmissionLifecycle() throws Exception {
        UUID teamId = UUID.randomUUID();
        UUID problemId = UUID.randomUUID();

        // 1. Create submission
        SubmitCodeRequestDTO request = new SubmitCodeRequestDTO(
                teamId, problemId, co.uceva.shared.domain.ProgrammingLanguage.PYTHON, "print(1)"
        );
        MvcResult createResult = mockMvc.perform(post("/api/v1/submissions")
                        .with(user("student").roles("STUDENT"))
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
