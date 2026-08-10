package co.uceva.problem.application.usecase;

import java.util.UUID;

/**
 * Puerto de entrada para el caso de uso de eliminación de un caso de prueba.
 */
public interface DeleteTestCaseUseCase {
    /**
     * Ejecuta la eliminación de un caso de prueba.
     *
     * @param testCaseId Identificador del caso de prueba a eliminar.
     */
    void execute(UUID testCaseId);
}
