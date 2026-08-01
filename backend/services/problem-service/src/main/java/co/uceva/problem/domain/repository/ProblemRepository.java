package co.uceva.problem.domain.repository;

import co.uceva.problem.domain.model.Problem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProblemRepository {
    Problem save(Problem problem);
    void deleteById(UUID problemId);
    Optional<Problem> findById(UUID problemId);
    List<Problem> findAllByCreatedBy(UUID createdBy);
    List<Problem> findAllByTitle(String title);
    List<Problem> findAll();
}