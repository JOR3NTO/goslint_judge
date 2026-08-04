package co.uceva.problem.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import co.uceva.problem.infrastructure.persistence.entity.TestCaseEntity;

public interface SpringDataTestCaseRepository extends JpaRepository<TestCaseEntity, UUID> {
    List<TestCaseEntity> findByProblemIdOrderByOrderIndexAsc(UUID problemId);

    void deleteByProblemId(UUID problemId);
}
