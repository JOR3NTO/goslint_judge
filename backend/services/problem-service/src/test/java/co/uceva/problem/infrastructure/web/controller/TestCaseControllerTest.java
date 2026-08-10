package co.uceva.problem.infrastructure.web.controller;

import co.uceva.problem.application.usecase.*;
import co.uceva.problem.domain.model.TestCase;
import co.uceva.problem.fixtures.ProblemFixtures;
import co.uceva.problem.infrastructure.web.dto.CreateTestCaseBatchRequestDTO;
import co.uceva.problem.infrastructure.web.dto.CreateTestCaseRequestDTO;
import co.uceva.problem.infrastructure.web.dto.DeleteTestCaseBatchRequestDTO;
import co.uceva.problem.infrastructure.web.dto.ReorderTestCasesRequestDTO;
import co.uceva.problem.infrastructure.web.dto.UpdateTestCaseRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TestCaseController.class)
class TestCaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateTestCaseUseCase createTestCaseUseCase;

    @MockBean
    private CreateTestCaseBatchUseCase createTestCaseBatchUseCase;

    @MockBean
    private UpdateTestCaseUseCase updateTestCaseUseCase;

    @MockBean
    private ReorderTestCasesUseCase reorderTestCaseUseCase;

    @MockBean
    private DeleteTestCaseUseCase deleteTestCaseUseCase;

    @MockBean
    private DeleteTestCaseBatchUseCase deleteTestCaseBatchUseCase;

    @MockBean
    private GetAllTestCaseByProblemIdUseCase getAllTestCaseByProblemIdUseCase;

    @MockBean
    private GetTestCaseByIdUseCase getTestCaseByIdUseCase;

    private final UUID problemId = UUID.randomUUID();
    private final UUID testCaseId = UUID.randomUUID();

    @Test
    void shouldCreateTestCase() throws Exception {
        CreateTestCaseRequestDTO request = ProblemFixtures.createTestCaseRequest();
        TestCase testCase = ProblemFixtures.aTestCase(problemId, 1);
        testCase.setId(testCaseId);
        when(createTestCaseUseCase.execute(any())).thenReturn(testCase);

        mockMvc.perform(post("/api/v1/problems/test-cases/{problemId}", problemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(testCaseId.toString()));
    }

    @Test
    void shouldGetAllTestCasesByProblemId() throws Exception {
        TestCase testCase = ProblemFixtures.aTestCase(problemId, 1);
        when(getAllTestCaseByProblemIdUseCase.execute(problemId)).thenReturn(List.of(testCase));

        mockMvc.perform(get("/api/v1/problems/test-cases/{problemId}/all", problemId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shouldGetTestCaseById() throws Exception {
        TestCase testCase = ProblemFixtures.aTestCase(problemId, 1);
        testCase.setId(testCaseId);
        when(getTestCaseByIdUseCase.execute(testCaseId)).thenReturn(testCase);

        mockMvc.perform(get("/api/v1/problems/test-cases/{id}", testCaseId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(testCaseId.toString()));
    }

    @Test
    void shouldUpdateTestCase() throws Exception {
        UpdateTestCaseRequestDTO request = ProblemFixtures.updateTestCaseRequest();
        TestCase testCase = ProblemFixtures.aTestCase(problemId, 2);
        testCase.setId(testCaseId);
        when(updateTestCaseUseCase.execute(any())).thenReturn(testCase);

        mockMvc.perform(put("/api/v1/problems/test-cases/{problemId}/{testCaseId}", problemId, testCaseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testCaseId.toString()));
    }

    @Test
    void shouldReorderTestCases() throws Exception {
        ReorderTestCasesRequestDTO request = new ReorderTestCasesRequestDTO(List.of(UUID.randomUUID(), UUID.randomUUID()));
        doNothing().when(reorderTestCaseUseCase).execute(any(), any());

        mockMvc.perform(put("/api/v1/problems/test-cases/{problemId}/reorder", problemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldDeleteTestCase() throws Exception {
        doNothing().when(deleteTestCaseUseCase).execute(testCaseId);

        mockMvc.perform(delete("/api/v1/problems/test-cases/{problemId}/{testCaseId}", problemId, testCaseId))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldCreateTestCaseBatch() throws Exception {
        CreateTestCaseBatchRequestDTO request = ProblemFixtures.createTestCaseBatchRequest();
        TestCase testCase1 = ProblemFixtures.aTestCase(problemId, 1);
        testCase1.setId(UUID.randomUUID());
        TestCase testCase2 = ProblemFixtures.aTestCase(problemId, 2);
        testCase2.setId(UUID.randomUUID());
        when(createTestCaseBatchUseCase.execute(any())).thenReturn(List.of(testCase1, testCase2));

        mockMvc.perform(post("/api/v1/problems/test-cases/{problemId}/batch", problemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldDeleteTestCaseBatch() throws Exception {
        DeleteTestCaseBatchRequestDTO request = ProblemFixtures.deleteTestCaseBatchRequest(List.of(testCaseId));
        doNothing().when(deleteTestCaseBatchUseCase).execute(any());

        mockMvc.perform(post("/api/v1/problems/test-cases/batch-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }
}
