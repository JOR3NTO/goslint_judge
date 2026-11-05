package co.uceva.problem.domain.repository;

import co.uceva.problem.domain.model.Problem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida (contrato de repositorio) para la entidad {@link Problem}.
 * Define las operaciones de persistencia sin acoplarse a una tecnología específica.
 */
public interface ProblemRepository {
    /** Guarda un problema y retorna la instancia persistida. */
    Problem save(Problem problem);
    /** Elimina un problema dado su identificador. */
    void deleteById(UUID problemId);
    /** Busca un problema por su identificador. */
    Optional<Problem> findById(UUID problemId);
    /** Busca todos los problemas creados por un usuario específico. */
    List<Problem> findAllByCreatedBy(UUID createdBy);
    /** Busca problemas cuyo título coincida parcialmente con el valor dado. */
    List<Problem> findAllByTitle(String title);
    /** Recupera todos los problemas registrados. */
    List<Problem> findAll();
}