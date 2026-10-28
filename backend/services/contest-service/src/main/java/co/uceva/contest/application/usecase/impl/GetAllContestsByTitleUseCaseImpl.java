package co.uceva.contest.application.usecase.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contest.application.usecase.GetAllContestsByTitleUseCase;
import co.uceva.contest.domain.model.Contest;
import co.uceva.contest.domain.repository.ContestRepository;

/**
 * Servicio de Aplicación que implementa la búsqueda de concursos por título.
 */
@Service
public class GetAllContestsByTitleUseCaseImpl implements GetAllContestsByTitleUseCase {

    private final ContestRepository contestRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param contestRepository Puerto de salida para consultar concursos.
     */
    public GetAllContestsByTitleUseCaseImpl(ContestRepository contestRepository) {
        this.contestRepository = contestRepository;
    }

    /**
     * Recupera los concursos cuyo título coincida con el criterio dado.
     *
     * @param title Título o fragmento a buscar.
     * @return Lista de concursos que coinciden.
     */
    @Override
    @Transactional(readOnly = true)
    public List<Contest> execute(String title) {
        return contestRepository.findAllByTitle(title);
    }
}
