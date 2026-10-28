package co.uceva.contest.application.usecase;

import java.util.List;

import co.uceva.contest.domain.model.Contest;

/**
 * Puerto de entrada para el caso de uso de búsqueda de concursos por título.
 */
public interface GetAllContestsByTitleUseCase {
    /**
     * Busca concursos cuyo título coincida con el criterio dado.
     *
     * @param title Título o fragmento del título a buscar.
     * @return Lista de concursos que coinciden con el criterio.
     */
    List<Contest> execute(String title);
}
