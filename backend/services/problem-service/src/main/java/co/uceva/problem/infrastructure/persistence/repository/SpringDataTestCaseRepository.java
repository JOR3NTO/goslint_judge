package co.uceva.problem.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import co.uceva.problem.infrastructure.persistence.entity.TestCaseEntity;

/**
 * Repositorio de Spring Data JPA para la entidad {@link TestCaseEntity}.
 * Proporciona operaciones CRUD y consultas derivadas por nombre.
 */
public interface SpringDataTestCaseRepository extends JpaRepository<TestCaseEntity, UUID> {
    /** Busca los casos de prueba de un problema ordenados por índice ascendente. */
    List<TestCaseEntity> findByProblemIdOrderByOrderIndexAsc(UUID problemId);

    /**
     * Busca los casos de prueba de ejemplo ({@code isSample = true}) de un problema,
     * ordenados por índice ascendente.
     *
     * @param problemId Identificador del problema.
     * @return Lista de casos de prueba de ejemplo.
     */
    List<TestCaseEntity> findByProblemIdAndIsSampleTrueOrderByOrderIndexAsc(UUID problemId);

    /** Elimina todos los casos de prueba asociados a un problema. */
    void deleteByProblemId(UUID problemId);
}
