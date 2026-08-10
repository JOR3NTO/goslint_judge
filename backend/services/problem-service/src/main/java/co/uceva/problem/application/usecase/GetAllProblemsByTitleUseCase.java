package co.uceva.problem.application.usecase;

import java.util.List;

import co.uceva.problem.domain.model.Problem;

/**
 * Puerto de entrada para el caso de uso de búsqueda de problemas por título.
 */
public interface GetAllProblemsByTitleUseCase {
    /**
     * Busca problemas cuyo título coincida con el criterio dado.
     *
     * @param title Título o fragmento del título a buscar.
     * @return Lista de problemas que coinciden con el criterio.
     */
    List<Problem> execute(String title);
}
