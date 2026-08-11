package co.uceva.submission.domain.repository;

import co.uceva.submission.domain.model.Submission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida (contrato de repositorio) para la entidad {@link Submission}.
 * <p>
 * Define las operaciones de persistencia necesarias para gestionar los envíos
 * de código fuente, sin acoplarse a una tecnología de almacenamiento específica.
 * </p>
 */
public interface SubmissionRepository {

    /** Guarda un envío y retorna la instancia persistida. */
    Submission save(Submission submission);

    /** Busca un envío por su identificador único. */
    Optional<Submission> findById(UUID submissionId);

    /** Recupera todos los envíos asociados a un problema específico. */
    List<Submission> findByProblemId(UUID problemId);

    /** Recupera todos los envíos realizados por un equipo específico. */
    List<Submission> findByTeamId(UUID teamId);

    /**
     * Recupera los envíos realizados por un equipo específico para un problema específico.
     *
     * @param problemId Identificador del problema.
     * @param teamId    Identificador del equipo.
     * @return Lista de envíos del equipo en el problema indicado.
     */
    List<Submission> findByProblemIdAndTeamId(UUID problemId, UUID teamId);

    /** Recupera todos los envíos registrados en el sistema. */
    List<Submission> findAll();

    /** Elimina un envío dado su identificador. */
    void deleteById(UUID submissionId);

    /**
     * Verifica si ya existe un envío idéntico realizado por el mismo equipo
     * para el mismo problema y con el mismo código fuente.
     *
     * @param teamId     Identificador del equipo.
     * @param problemId  Identificador del problema.
     * @param sourceCode Código fuente en texto plano.
     * @return {@code true} si ya existe un envío duplicado; {@code false} en caso contrario.
     */
    boolean existsByTeamIdAndProblemIdAndSourceCode(UUID teamId, UUID problemId, String sourceCode);
}
