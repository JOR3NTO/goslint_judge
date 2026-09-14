package co.uceva.submission.infrastructure.persistence.repository;

import co.uceva.shared.domain.ProgrammingLanguage;
import co.uceva.shared.domain.SubmissionStatus;
import co.uceva.submission.infrastructure.persistence.entity.SubmissionEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repositorio de Spring Data JPA para la entidad {@link SubmissionEntity}.
 * Proporciona operaciones CRUD y consultas derivadas por nombre.
 */
public interface SpringDataSubmissionRepository extends JpaRepository<SubmissionEntity, UUID> {

    /** Recupera todos los envíos asociados a un problema específico. */
    List<SubmissionEntity> findByProblemId(UUID problemId);

    /** Recupera todos los envíos realizados por un equipo específico. */
    List<SubmissionEntity> findByTeamId(UUID teamId);

    /** Recupera los envíos de un equipo para un problema específico. */
    List<SubmissionEntity> findByProblemIdAndTeamId(UUID problemId, UUID teamId);

    /**
     * Verifica si ya existe un envío idéntico realizado por el mismo equipo
     * para el mismo problema, con el mismo código fuente y en el mismo lenguaje
     * de programación.
     */
    boolean existsByTeamIdAndProblemIdAndSourceCodeAndLanguage(
            UUID teamId, UUID problemId, String sourceCode, ProgrammingLanguage language);

    /**
     * Recupera los envíos en un estado dado recibidos antes de un instante,
     * del más antiguo al más reciente. El {@link Pageable} acota el tamaño del lote.
     */
    List<SubmissionEntity> findByStatusAndSubmittedAtBeforeOrderBySubmittedAtAsc(
            SubmissionStatus status, Instant submittedBefore, Pageable pageable);
}
