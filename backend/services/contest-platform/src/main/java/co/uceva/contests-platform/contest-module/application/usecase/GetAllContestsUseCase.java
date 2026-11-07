package co.uceva.contestsplatform.contestmodule.application.usecase;

import java.util.List;

import co.uceva.contestsplatform.contestmodule.domain.model.Contest;

/**
 * Puerto de entrada para el caso de uso de consulta de todos los concursos.
 */
public interface GetAllContestsUseCase {
    /**
     * Recupera todos los concursos registrados en el sistema.
     *
     * @return Lista de concursos disponibles.
     */
    List<Contest> execute();
}
