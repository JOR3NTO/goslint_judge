package co.uceva.contest.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import co.uceva.contest.infrastructure.persistence.entity.ContestProblemEntity;

/**
 * Repositorio de Spring Data JPA para la entidad {@link ContestProblemEntity}.
 */
public interface SpringDataContestProblemRepository extends JpaRepository<ContestProblemEntity, UUID> {
    List<ContestProblemEntity> findByContestIdOrderByOrderIndexAsc(UUID contestId);

    boolean existsByContestIdAndProblemId(UUID contestId, UUID problemId);

    boolean existsByContestIdAndAlias(UUID contestId, String alias);
}
