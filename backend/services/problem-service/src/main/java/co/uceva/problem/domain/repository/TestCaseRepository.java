package co.uceva.problem.domain.repository;

import co.uceva.problem.domain.model.TestCase;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida (contrato de repositorio) para la entidad {@link TestCase}.
 * Define las operaciones de persistencia sin acoplarse a una tecnología específica.
 */
public interface TestCaseRepository {
    /** Guarda un caso de prueba y retorna la instancia persistida. */
    TestCase save(TestCase testCase);
    /** Guarda una lista de casos de prueba de forma masiva. */
    List<TestCase> saveAll(List<TestCase> testCases);
    /** Elimina un caso de prueba dado su identificador. */
    void deleteById(UUID testCaseId);
    /** Elimina varios casos de prueba dados sus identificadores. */
    void deleteAllById(List<UUID> testCaseIds);
    /** Elimina todos los casos de prueba asociados a un problema. */
    void deleteByProblemId(UUID problemId);
    /** Busca un caso de prueba por su identificador. */
    Optional<TestCase> findById(UUID testCaseId);
    /** Recupera todos los casos de prueba asociados a un problema. */
    List<TestCase> findAllByProblemId(UUID problemId);
    /**
     * Recupera únicamente los casos de prueba marcados como ejemplo
     * ({@code isSample = true}) asociados a un problema.
     *
     * @param problemId Identificador del problema.
     * @return Lista de casos de prueba de ejemplo del problema.
     */
    List<TestCase> findAllSampleByProblemId(UUID problemId);
    /** Actualiza los índices de orden de una lista de casos de prueba. */
    void updateOrderIndexes(Map<UUID, Integer> newOrders);
}