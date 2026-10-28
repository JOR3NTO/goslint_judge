package co.uceva.contest.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import co.uceva.contest.infrastructure.persistence.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de Spring Data JPA para la entidad {@link MemberEntity}.
 */
public interface SpringDataMemberRepository extends JpaRepository<MemberEntity, UUID> {
    List<MemberEntity> findByTeamId(UUID teamId);

    List<MemberEntity> findByUserId(UUID userId);

    boolean existsByTeamIdAndUserId(UUID teamId, UUID userId);

    // Borrado directo en BD: un em.remove() lo deshace la cascada de TeamEntity.members
    // si el equipo ya está cargado en la misma transacción.
    @Modifying
    @Query("delete from MemberEntity m where m.teamId = :teamId and m.userId = :userId")
    void deleteByTeamIdAndUserId(@Param("teamId") UUID teamId, @Param("userId") UUID userId);

    @Modifying
    @Query("delete from MemberEntity m where m.teamId = :teamId")
    void deleteByTeamId(@Param("teamId") UUID teamId);
}
