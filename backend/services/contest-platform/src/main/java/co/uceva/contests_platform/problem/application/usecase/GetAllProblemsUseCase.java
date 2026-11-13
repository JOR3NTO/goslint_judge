package co.uceva.contests_platform.problem.application.usecase;

import java.util.List;

import co.uceva.contests_platform.problem.domain.model.Problem;

/**
 * Puerto de entrada para el caso de uso de consulta de todos los problemas.
 */
public interface GetAllProblemsUseCase {
    /**
     * Recupera todos los problemas registrados en el sistema.
     *
     * @return Lista de problemas disponibles.
     */
    List<Problem> execute();
}
