package co.uceva.contest.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.uceva.contest.infrastructure.persistence.entity.TeamEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de Spring Data JPA para la entidad {@link TeamEntity}.
 */
public interface SpringDataTeamRepository extends JpaRepository<TeamEntity, UUID> {
    List<TeamEntity> findByContestId(UUID contestId);

    List<TeamEntity> findByContestIdOrderByScoreDescPenaltyTimeAsc(UUID contestId);

    List<TeamEntity> findByContestIdAndTeamNameContainingIgnoreCase(UUID contestId, String teamName);

    Optional<TeamEntity> findByContestIdAndTeamName(UUID contestId, String teamName);

    boolean existsByContestIdAndTeamName(UUID contestId, String teamName);

    @Query("select t from TeamEntity t join t.members m where m.userId = :userId")
    List<TeamEntity> findByUserId(@Param("userId") UUID userId);

    @Query("select t from TeamEntity t join t.members m where t.contestId = :contestId and m.userId = :userId")
    Optional<TeamEntity> findByContestIdAndUserId(@Param("contestId") UUID contestId, @Param("userId") UUID userId);

    @Query("select count(t) > 0 from TeamEntity t join t.members m where t.contestId = :contestId and m.userId = :userId")
    boolean existsByContestIdAndUserId(@Param("contestId") UUID contestId, @Param("userId") UUID userId);
}
