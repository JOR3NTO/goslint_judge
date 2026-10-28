package co.uceva.contest.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.uceva.contest.domain.model.ContestProblem;

/**
 * Puerto de salida (contrato de repositorio) para la entidad {@link ContestProblem}.
 * Define las operaciones de persistencia sin acoplarse a una tecnología específica.
 */
public interface ContestProblemRepository {

    /**
     * Guarda un problema de concurso y retorna la instancia persistida.
     *
     * @param contestProblem Problema de concurso a persistir.
     * @return El problema de concurso persistido.
     */
    ContestProblem save(ContestProblem contestProblem);

    /**
     * Busca un problema de concurso por su identificador único.
     *
     * @param id Identificador del problema de concurso.
     * @return {@link Optional} con el problema si existe, o vacío en caso contrario.
     */
    Optional<ContestProblem> findById(UUID id);

    /**
     * Recupera los problemas de un concurso ordenados por su posición ascendente.
     *
     * @param contestId Identificador del concurso.
     * @return Lista de problemas del concurso en orden de presentación.
     */
    List<ContestProblem> findAllByContestIdOrderByOrderIndexAsc(UUID contestId);

    /**
     * Elimina un problema de concurso dado su identificador único.
     *
     * @param id Identificador del problema de concurso a eliminar.
     */
    void deleteById(UUID id);

    /**
     * Verifica si un problema ya está asignado a un concurso.
     *
     * @param contestId Identificador del concurso.
     * @param problemId Identificador del problema.
     * @return {@code true} si el problema ya está en el concurso.
     */
    boolean existsByContestIdAndProblemId(UUID contestId, UUID problemId);

    /**
     * Verifica si un alias ya está en uso dentro de un concurso.
     *
     * @param contestId Identificador del concurso.
     * @param alias     Alias normalizado en mayúsculas.
     * @return {@code true} si el alias ya está en uso en el concurso.
     */
    boolean existsByContestIdAndAlias(UUID contestId, String alias);
}
