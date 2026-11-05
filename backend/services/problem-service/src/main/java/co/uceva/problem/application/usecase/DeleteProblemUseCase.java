package co.uceva.problem.application.usecase;

import java.util.UUID;

/**
 * Puerto de entrada para el caso de uso de eliminación de un problema.
 */
public interface DeleteProblemUseCase {
    /**
     * Ejecuta la eliminación de un problema.
     *
     * @param problemId Identificador del problema a eliminar.
     */
    void execute(UUID problemId);
}
