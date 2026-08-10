package co.uceva.problem.infrastructure.web.controller;

import co.uceva.problem.application.usecase.*;
import co.uceva.problem.domain.model.TestCase;
import co.uceva.problem.infrastructure.web.dto.*;
import co.uceva.problem.infrastructure.web.mapper.TestCaseWebMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1/problems/test-cases")
public class TestCaseController {

    private final CreateTestCaseUseCase createTestCaseUseCase;
    private final CreateTestCaseBatchUseCase createTestCaseBatchUseCase;
    private final UpdateTestCaseUseCase updateTestCaseUseCase;
    private final ReorderTestCasesUseCase reorderTestCaseUseCase;
    private final DeleteTestCaseUseCase deleteTestCaseUseCase;
    private final DeleteTestCaseBatchUseCase deleteTestCaseBatchUseCase;
    private final GetAllTestCaseByProblemIdUseCase getAllTestCaseByProblemIdUseCase;
    private final GetTestCaseByIdUseCase getTestCaseByIdUseCase;

    public TestCaseController(
            CreateTestCaseUseCase createTestCaseUseCase,
            CreateTestCaseBatchUseCase createTestCaseBatchUseCase,
            UpdateTestCaseUseCase updateTestCaseUseCase,
            ReorderTestCasesUseCase reorderTestCaseUseCase,
            DeleteTestCaseUseCase deleteTestCaseUseCase,
            DeleteTestCaseBatchUseCase deleteTestCaseBatchUseCase,
            GetAllTestCaseByProblemIdUseCase getAllTestCaseByProblemIdUseCase,
            GetTestCaseByIdUseCase getTestCaseByIdUseCase) {
        this.createTestCaseUseCase = createTestCaseUseCase;
        this.createTestCaseBatchUseCase = createTestCaseBatchUseCase;
        this.updateTestCaseUseCase = updateTestCaseUseCase;
        this.reorderTestCaseUseCase = reorderTestCaseUseCase;
        this.deleteTestCaseUseCase = deleteTestCaseUseCase;
        this.deleteTestCaseBatchUseCase = deleteTestCaseBatchUseCase;
        this.getAllTestCaseByProblemIdUseCase = getAllTestCaseByProblemIdUseCase;
        this.getTestCaseByIdUseCase = getTestCaseByIdUseCase;
    }

    @PostMapping("/{problemId}")
    public ResponseEntity<TestCaseResponseDTO> create(
            @PathVariable("problemId") UUID problemId,
            @RequestBody CreateTestCaseRequestDTO request) {
        var command = TestCaseWebMapper.toCommand(problemId, request);
        TestCase created = createTestCaseUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(TestCaseWebMapper.toResponse(created));
    }

    @GetMapping("/{problemId}/all")
    public ResponseEntity<List<TestCaseResponseDTO>> getAll(@PathVariable("problemId") UUID problemId) {
        List<TestCase> testCases = getAllTestCaseByProblemIdUseCase.execute(problemId);
        List<TestCaseResponseDTO> testCasesResponce = testCases.stream()
                .map(TestCaseWebMapper::toResponse)
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(testCasesResponce);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TestCaseResponseDTO> getById(@PathVariable("id") UUID id) {
        TestCase testCase = getTestCaseByIdUseCase.execute(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(TestCaseWebMapper.toResponse(testCase));
    }

    @PutMapping("/{problemId}/{testCaseId}")
    public ResponseEntity<TestCaseResponseDTO> update(
            @PathVariable("problemId") UUID problemId,
            @PathVariable("testCaseId") UUID testCaseId,
            @RequestBody UpdateTestCaseRequestDTO request) {
        var command = TestCaseWebMapper.toCommand(testCaseId, request);
        TestCase updated = updateTestCaseUseCase.execute(command);
        return ResponseEntity.ok(TestCaseWebMapper.toResponse(updated));
    }

    @PutMapping("/{problemId}/reorder")
    public ResponseEntity<Void> reorder(
            @PathVariable("problemId") UUID problemId,
            @RequestBody ReorderTestCasesRequestDTO request) {
        reorderTestCaseUseCase.execute(problemId, request.testCaseIdsInOrder());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{problemId}/{testCaseId}")
    public ResponseEntity<Void> delete(
            @PathVariable("problemId") UUID problemId,
            @PathVariable("testCaseId") UUID testCaseId) {
        deleteTestCaseUseCase.execute(testCaseId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{problemId}/batch")
    public ResponseEntity<List<TestCaseResponseDTO>> createBatch(
            @PathVariable("problemId") UUID problemId,
            @RequestBody CreateTestCaseBatchRequestDTO request) {
        var command = TestCaseWebMapper.toCreateBatchCommand(problemId, request);
        List<TestCase> created = createTestCaseBatchUseCase.execute(command);
        List<TestCaseResponseDTO> response = created.stream()
                .map(TestCaseWebMapper::toResponse)
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/batch-delete")
    public ResponseEntity<Void> deleteBatch(@RequestBody DeleteTestCaseBatchRequestDTO request) {
        deleteTestCaseBatchUseCase.execute(request.testCaseIds());
        return ResponseEntity.noContent().build();
    }
}