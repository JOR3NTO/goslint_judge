package co.uceva.problem.application.usecase;

import java.util.List;
import java.util.UUID;

import co.uceva.problem.domain.model.TestCase;

/**
 * Puerto de entrada para el caso de uso de consulta de casos de prueba
 * de ejemplo ({@code isSample = true}) asociados a un problema.
 */
public interface GetAllSampleTestCasesByProblemIdUseCase {

    /**
     * Recupera los casos de prueba de ejemplo de un problema.
     *
     * @param problemId Identificador del problema.
     * @return Lista de casos de prueba de ejemplo.
     */
    List<TestCase> execute(UUID problemId);
}
