package co.uceva.problem.application.usecase;

import java.util.List;
import java.util.UUID;

import co.uceva.problem.domain.model.TestCase;

/**
 * Puerto de entrada para el caso de uso de consulta de casos de prueba por problema.
 */
public interface GetAllTestCaseByProblemIdUseCase {
    /**
     * Recupera todos los casos de prueba asociados a un problema.
     *
     * @param problemId Identificador del problema.
     * @return Lista de casos de prueba del problema.
     */
    List<TestCase> execute(UUID problemId);
}
