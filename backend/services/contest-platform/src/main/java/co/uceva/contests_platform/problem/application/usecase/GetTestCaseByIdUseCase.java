package co.uceva.contests_platform.problem.application.usecase;

import java.util.UUID;

import co.uceva.contests_platform.problem.domain.model.TestCase;

/**
 * Puerto de entrada para el caso de uso de consulta de un caso de prueba por identificador.
 */
public interface GetTestCaseByIdUseCase {
    /**
     * Busca un caso de prueba por su identificador.
     *
     * @param testCaseId Identificador del caso de prueba.
     * @return El caso de prueba encontrado.
     */
    TestCase execute(UUID testCaseId);
}
