package co.uceva.contest.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import co.uceva.contest.infrastructure.persistence.entity.ContestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de Spring Data JPA para la entidad {@link ContestEntity}.
 * Proporciona operaciones CRUD y consultas derivadas por nombre.
 */
public interface SpringDataContestRepository extends JpaRepository<ContestEntity, UUID> {
    /** Busca concursos por el identificador de su creador. */
    List<ContestEntity> findByCreatedBy(UUID createdBy);

    /** Busca concursos cuyo título contenga el texto dado, sin distinguir mayúsculas. */
    List<ContestEntity> findByTitleContainingIgnoreCase(String title);
}
