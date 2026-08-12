package co.uceva.problem.application.usecase;

import java.util.UUID;

import co.uceva.problem.domain.model.Problem;

/**
 * Puerto de entrada para el caso de uso de consulta de un problema por identificador.
 */
public interface GetProblemByIdUseCase {
    /**
     * Busca un problema por su identificador.
     *
     * @param problemId Identificador del problema.
     * @return El problema encontrado.
     */
    Problem execute(UUID problemId);
}
