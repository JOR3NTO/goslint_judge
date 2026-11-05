package co.uceva.submission.domain.repository;

import co.uceva.shared.domain.ProgrammingLanguage;
import co.uceva.submission.domain.model.Submission;

import java.time.Instant;
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
     * Recupera los envíos que siguen pendientes de ser entregados al motor de
     * evaluación y que llevan esperando el tiempo suficiente como para
     * considerarlos rezagados.
     * <p>
     * Es la consulta que alimenta el reintento automático de encolamiento: una
     * fila en estado {@code PENDING} representa trabajo de entrega que quedó sin
     * completar, por ejemplo porque el broker estaba caído.
     * </p>
     *
     * @param submittedBefore Solo se devuelven envíos recibidos antes de este instante.
     * @param limit           Número máximo de envíos a recuperar, ordenados del más antiguo al más reciente.
     * @return Lista de envíos pendientes de encolar.
     */
    List<Submission> findStalePending(Instant submittedBefore, int limit);

    /**
     * Verifica si ya existe un envío idéntico realizado por el mismo equipo
     * para el mismo problema, con el mismo código fuente y en el mismo lenguaje
     * de programación.
     *
     * @param teamId     Identificador del equipo.
     * @param problemId  Identificador del problema.
     * @param sourceCode Código fuente en texto plano.
     * @param language   Lenguaje de programación del código fuente.
     * @return {@code true} si ya existe un envío duplicado; {@code false} en caso contrario.
     */
    boolean existsByTeamIdAndProblemIdAndSourceCodeAndLanguage(
            UUID teamId, UUID problemId, String sourceCode, ProgrammingLanguage language);
}
