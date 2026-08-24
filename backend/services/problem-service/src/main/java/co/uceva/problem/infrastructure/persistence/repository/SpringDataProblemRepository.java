package co.uceva.problem.infrastructure.persistence.repository;

import co.uceva.problem.infrastructure.persistence.entity.ProblemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

/**
 * Repositorio de Spring Data JPA para la entidad {@link ProblemEntity}.
 * Proporciona operaciones CRUD y consultas derivadas por nombre.
 */
public interface SpringDataProblemRepository extends JpaRepository<ProblemEntity, UUID> {
    /** Busca problemas por el identificador de su creador. */
    List<ProblemEntity> findByCreatedBy(UUID createdBy);

    /** Busca problemas cuyo título contenga el texto dado, sin distinguir mayúsculas. */
    List<ProblemEntity> findByTitleContainingIgnoreCase(String title);

    /** Verifica si existe un problema con el título dado, sin distinguir mayúsculas. */
    boolean existsByTitleIgnoreCase(String title);
}