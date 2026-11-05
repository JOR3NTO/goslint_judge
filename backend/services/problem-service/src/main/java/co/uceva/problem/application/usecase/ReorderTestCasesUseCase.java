package co.uceva.problem.application.usecase;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de entrada para el caso de uso de reordenamiento de casos de prueba.
 */
public interface ReorderTestCasesUseCase {
    /**
     * Actualiza el orden de ejecución de los casos de prueba de un problema.
     *
     * @param problemId          Identificador del problema.
     * @param orderedTestCaseIds Lista de identificadores en el nuevo orden deseado.
     */
    void execute(UUID problemId, List<UUID> orderedTestCaseIds);
}
