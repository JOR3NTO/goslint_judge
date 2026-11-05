package co.uceva.problem.application.usecase;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de entrada para el caso de uso de eliminación masiva de casos de prueba.
 */
public interface DeleteTestCaseBatchUseCase {
    /**
     * Ejecuta la eliminación de varios casos de prueba.
     *
     * @param testCaseIds Lista de identificadores de los casos de prueba a eliminar.
     */
    void execute(List<UUID> testCaseIds);
}
