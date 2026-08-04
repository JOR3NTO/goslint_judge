package co.uceva.problem.infrastructure.persistence.repository;

import co.uceva.problem.infrastructure.persistence.entity.ProblemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SpringDataProblemRepository extends JpaRepository<ProblemEntity, UUID> {
    List<ProblemEntity> findByCreatedBy(UUID createdBy);

    List<ProblemEntity> findByTitleContainingIgnoreCase(String title);

    boolean existsByTitleIgnoreCase(String title);
}