package co.uceva.problem;

import co.uceva.problem.fixtures.ProblemFixtures;
import co.uceva.problem.infrastructure.web.dto.CreateTestCaseBatchRequestDTO;
import co.uceva.problem.infrastructure.web.dto.CreateTestCaseRequestDTO;
import co.uceva.problem.infrastructure.web.dto.DeleteTestCaseBatchRequestDTO;
import co.uceva.problem.infrastructure.web.dto.ReorderTestCasesRequestDTO;
import co.uceva.problem.infrastructure.web.dto.UpdateProblemRequestDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProblemServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldExecuteFullProblemLifecycle() throws Exception {
        // 1. Create problem
        MvcResult createResult = mockMvc.perform(post("/api/v1/problems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ProblemFixtures.createProblemRequest())))
                .andExpect(status().isCreated())
                .andReturn();
        UUID problemId = extractId(createResult);

        // 2. Get problem by id
        mockMvc.perform(get("/api/v1/problems/{id}", problemId))
                .andExpect(status().isOk());

        // 3. Create test cases
        CreateTestCaseRequestDTO caseRequest = ProblemFixtures.createTestCaseRequest();
        MvcResult firstCaseResult = mockMvc.perform(post("/api/v1/problems/test-cases/{problemId}", problemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(caseRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID firstCaseId = extractId(firstCaseResult);

        CreateTestCaseRequestDTO secondCase = new CreateTestCaseRequestDTO(
                2, "3 4", "7", "7", false
        );
        MvcResult secondCaseResult = mockMvc.perform(post("/api/v1/problems/test-cases/{problemId}", problemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondCase)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID secondCaseId = extractId(secondCaseResult);

        // 4. Reorder test cases (reverse)
        ReorderTestCasesRequestDTO reorderRequest = new ReorderTestCasesRequestDTO(List.of(secondCaseId, firstCaseId));
        mockMvc.perform(put("/api/v1/problems/test-cases/{problemId}/reorder", problemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reorderRequest)))
                .andExpect(status().isNoContent());

        // 5. Verify order
        MvcResult listResult = mockMvc.perform(get("/api/v1/problems/test-cases/{problemId}/all", problemId))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode cases = objectMapper.readTree(listResult.getResponse().getContentAsString());
        assertThat(cases.get(0).get("id").asText()).isEqualTo(secondCaseId.toString());
        assertThat(cases.get(1).get("id").asText()).isEqualTo(firstCaseId.toString());

        // 6. Update problem
        UpdateProblemRequestDTO updateRequest = ProblemFixtures.updateProblemRequest();
        mockMvc.perform(put("/api/v1/problems/{id}", problemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        // 7. Delete first test case
        mockMvc.perform(delete("/api/v1/problems/test-cases/{problemId}/{testCaseId}", problemId, firstCaseId))
                .andExpect(status().isNoContent());

        // 8. Delete problem
        mockMvc.perform(delete("/api/v1/problems/{id}", problemId))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldExecuteBatchTestCaseOperations() throws Exception {
        // 1. Create problem
        MvcResult createResult = mockMvc.perform(post("/api/v1/problems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ProblemFixtures.createProblemRequest())))
                .andExpect(status().isCreated())
                .andReturn();
        UUID problemId = extractId(createResult);

        // 2. Batch create (replace all)
        CreateTestCaseBatchRequestDTO batchRequest = ProblemFixtures.createTestCaseBatchRequest();
        MvcResult batchResult = mockMvc.perform(post("/api/v1/problems/test-cases/{problemId}/batch", problemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode batchCases = objectMapper.readTree(batchResult.getResponse().getContentAsString());
        assertThat(batchCases).hasSize(2);
        UUID firstCaseId = UUID.fromString(batchCases.get(0).get("id").asText());
        UUID secondCaseId = UUID.fromString(batchCases.get(1).get("id").asText());

        // 3. Verify order
        MvcResult listResult = mockMvc.perform(get("/api/v1/problems/test-cases/{problemId}/all", problemId))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode cases = objectMapper.readTree(listResult.getResponse().getContentAsString());
        assertThat(cases.get(0).get("orderIndex").asInt()).isEqualTo(1);
        assertThat(cases.get(1).get("orderIndex").asInt()).isEqualTo(2);

        // 4. Batch delete one case
        DeleteTestCaseBatchRequestDTO deleteRequest = new DeleteTestCaseBatchRequestDTO(List.of(firstCaseId));
        mockMvc.perform(post("/api/v1/problems/test-cases/batch-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteRequest)))
                .andExpect(status().isNoContent());

        // 5. Verify recalculated order
        MvcResult remainingResult = mockMvc.perform(get("/api/v1/problems/test-cases/{problemId}/all", problemId))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode remaining = objectMapper.readTree(remainingResult.getResponse().getContentAsString());
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).get("id").asText()).isEqualTo(secondCaseId.toString());
        assertThat(remaining.get(0).get("orderIndex").asInt()).isEqualTo(1);

        // 6. Cleanup
        mockMvc.perform(delete("/api/v1/problems/{id}", problemId))
                .andExpect(status().isNoContent());
    }

    private UUID extractId(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(json.get("id").asText());
    }
}
