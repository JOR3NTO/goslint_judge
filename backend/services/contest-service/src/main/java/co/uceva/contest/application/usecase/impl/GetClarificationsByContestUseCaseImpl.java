package co.uceva.contest.application.usecase.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contest.application.usecase.GetClarificationsByContestUseCase;
import co.uceva.contest.domain.exception.ContestNotFoundException;
import co.uceva.contest.domain.model.Clarification;
import co.uceva.contest.domain.repository.ClarificationRepository;
import co.uceva.contest.domain.repository.ContestRepository;

/**
 * Servicio de Aplicación que implementa el caso de uso: consultar todas las aclaraciones de un concurso.
 */
@Service
public class GetClarificationsByContestUseCaseImpl implements GetClarificationsByContestUseCase {

    private final ClarificationRepository clarificationRepository;
    private final ContestRepository contestRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param clarificationRepository Puerto de salida para consultar aclaraciones.
     * @param contestRepository       Puerto de salida para consultar concursos.
     */
    public GetClarificationsByContestUseCaseImpl(ClarificationRepository clarificationRepository,
                                                 ContestRepository contestRepository) {
        this.clarificationRepository = clarificationRepository;
        this.contestRepository = contestRepository;
    }

    /**
     * Consultar las aclaraciones de un concurso.
     *
     * @param contestId Identificador del concurso.
     * @return Lista de resultados.
     * @throws ContestNotFoundException Si el concurso no existe.
     */
    @Override
    @Transactional(readOnly = true)
    public List<Clarification> execute(UUID contestId) {
        if (contestRepository.findById(contestId).isEmpty()) {
            throw new ContestNotFoundException(contestId);
        }
        return clarificationRepository.findAllByContestId(contestId);
    }
}
