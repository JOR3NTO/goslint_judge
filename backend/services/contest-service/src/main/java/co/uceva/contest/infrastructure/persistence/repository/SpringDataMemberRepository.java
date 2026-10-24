package co.uceva.contest.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import co.uceva.contest.infrastructure.persistence.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de Spring Data JPA para la entidad {@link MemberEntity}.
 */
public interface SpringDataMemberRepository extends JpaRepository<MemberEntity, UUID> {
    List<MemberEntity> findByTeamId(UUID teamId);

    List<MemberEntity> findByUserId(UUID userId);

    boolean existsByTeamIdAndUserId(UUID teamId, UUID userId);

    void deleteByTeamIdAndUserId(UUID teamId, UUID userId);

    void deleteByTeamId(UUID teamId);
}
