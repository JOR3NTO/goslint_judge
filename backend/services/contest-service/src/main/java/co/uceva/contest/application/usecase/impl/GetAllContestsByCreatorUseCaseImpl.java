package co.uceva.contest.application.usecase.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contest.application.usecase.GetAllContestsByCreatorUseCase;
import co.uceva.contest.domain.model.Contest;
import co.uceva.contest.domain.repository.ContestRepository;

/**
 * Servicio de Aplicación que implementa la consulta de los concursos de un creador.
 */
@Service
public class GetAllContestsByCreatorUseCaseImpl implements GetAllContestsByCreatorUseCase {

    private final ContestRepository contestRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param contestRepository Puerto de salida para consultar concursos.
     */
    public GetAllContestsByCreatorUseCaseImpl(ContestRepository contestRepository) {
        this.contestRepository = contestRepository;
    }

    /**
     * Recupera los concursos creados por un usuario.
     *
     * @param createdBy Identificador del usuario creador.
     * @return Lista de concursos del creador.
     */
    @Override
    @Transactional(readOnly = true)
    public List<Contest> execute(UUID createdBy) {
        return contestRepository.findAllByCreatedBy(createdBy);
    }
}
