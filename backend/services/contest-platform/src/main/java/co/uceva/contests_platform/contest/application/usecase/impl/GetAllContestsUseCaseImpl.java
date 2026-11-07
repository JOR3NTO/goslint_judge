package co.uceva.contests_platform.contest.application.usecase.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contests_platform.contest.application.usecase.GetAllContestsUseCase;
import co.uceva.contests_platform.contest.domain.model.Contest;
import co.uceva.contests_platform.contest.domain.repository.ContestRepository;

/**
 * Servicio de Aplicación que implementa la consulta de todos los concursos.
 */
@Service
public class GetAllContestsUseCaseImpl implements GetAllContestsUseCase {

    private final ContestRepository contestRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param contestRepository Puerto de salida para consultar concursos.
     */
    public GetAllContestsUseCaseImpl(ContestRepository contestRepository) {
        this.contestRepository = contestRepository;
    }

    /**
     * Recupera todos los concursos registrados en el sistema.
     *
     * @return Lista completa de concursos.
     */
    @Override
    @Transactional(readOnly = true)
    public List<Contest> execute() {
        return contestRepository.findAll();
    }
}
