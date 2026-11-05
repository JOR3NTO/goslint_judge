package co.uceva.contest.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.uceva.contest.infrastructure.persistence.entity.ClarificationEntity;

/**
 * Repositorio de Spring Data JPA para la entidad {@link ClarificationEntity}.
 */
public interface SpringDataClarificationRepository extends JpaRepository<ClarificationEntity, UUID> {

    @Query("select c from ClarificationEntity c where c.problemId in "
            + "(select p.id from ContestProblemEntity p where p.contestId = :contestId) "
            + "order by c.createdAt desc")
    List<ClarificationEntity> findByContestId(@Param("contestId") UUID contestId);
}
