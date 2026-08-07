package co.uceva.problem.infrastructure.web.controller;

import co.uceva.problem.application.usecase.*;
import co.uceva.problem.domain.model.Problem;
import co.uceva.problem.infrastructure.web.dto.*;
import co.uceva.problem.infrastructure.web.mapper.ProblemWebMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1/problems")
public class ProblemController {

    private final CreateProblemUseCase createProblemUseCase;
    private final GetProblemByIdUseCase getProblemByIdUseCase;
    private final GetAllProblemsByTitleUseCase getAllProblemsByTitleUseCase;
    private final GetAllProblemsUseCase getAllProblemsUseCase;
    private final UpdateProblemUseCase updateProblemUseCase;
    private final DeleteProblemUseCase deleteProblemUseCase;

    public ProblemController(
            CreateProblemUseCase createProblemUseCase,
            GetProblemByIdUseCase getProblemByIdUseCase,
            GetAllProblemsByTitleUseCase getAllProblemsByTitleUseCase,
            GetAllProblemsUseCase getAllProblemsUseCase,
            UpdateProblemUseCase updateProblemUseCase,
            DeleteProblemUseCase deleteProblemUseCase) {
        this.createProblemUseCase = createProblemUseCase;
        this.getProblemByIdUseCase = getProblemByIdUseCase;
        this.getAllProblemsByTitleUseCase = getAllProblemsByTitleUseCase;
        this.getAllProblemsUseCase = getAllProblemsUseCase;
        this.updateProblemUseCase = updateProblemUseCase;
        this.deleteProblemUseCase = deleteProblemUseCase;
    }

    @PostMapping
    public ResponseEntity<ProblemResponseDTO> create(@RequestBody CreateProblemRequestDTO request) {
        var command = ProblemWebMapper.toCommand(request);
        Problem created = createProblemUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProblemWebMapper.toResponse(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProblemResponseDTO> getById(@PathVariable("id") UUID id) {
        Problem problem = getProblemByIdUseCase.execute(id);
        return ResponseEntity.ok(ProblemWebMapper.toResponse(problem));
    }

    @GetMapping("/title/{title}")
    public ResponseEntity<List<ProblemResponseDTO>> getAllByTitle(@PathVariable("title") String title) {
        List<Problem> problems = getAllProblemsByTitleUseCase.execute(title);
        List<ProblemResponseDTO> problemsResponce = problems.stream()
                .map(ProblemWebMapper::toResponse)
                .toList();
        return ResponseEntity.ok(problemsResponce);
    }

    @GetMapping("/all")
    public ResponseEntity<List<ProblemResponseDTO>> getAll() {
        List<Problem> problems = getAllProblemsUseCase.execute();
        List<ProblemResponseDTO> problemsResponce = problems.stream()
                .map(ProblemWebMapper::toResponse)
                .toList();
        return ResponseEntity.ok(problemsResponce);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProblemResponseDTO> update(
            @PathVariable("id") UUID id,
            @RequestBody UpdateProblemRequestDTO request) {
        var command = ProblemWebMapper.toCommand(id, request);
        Problem updated = updateProblemUseCase.execute(command);
        return ResponseEntity.ok(ProblemWebMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        deleteProblemUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}