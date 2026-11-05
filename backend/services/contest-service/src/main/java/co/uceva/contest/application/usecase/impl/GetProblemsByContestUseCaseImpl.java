package co.uceva.contest.application.usecase.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contest.application.usecase.GetProblemsByContestUseCase;
import co.uceva.contest.domain.exception.ContestNotFoundException;
import co.uceva.contest.domain.model.ContestProblem;
import co.uceva.contest.domain.repository.ContestProblemRepository;
import co.uceva.contest.domain.repository.ContestRepository;

/**
 * Servicio de Aplicación que implementa el caso de uso: consultar los problemas de un concurso.
 */
@Service
public class GetProblemsByContestUseCaseImpl implements GetProblemsByContestUseCase {

    private final ContestProblemRepository contestProblemRepository;
    private final ContestRepository contestRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param contestProblemRepository Puerto de salida para consultar problemas de concurso.
     * @param contestRepository        Puerto de salida para consultar concursos.
     */
    public GetProblemsByContestUseCaseImpl(ContestProblemRepository contestProblemRepository,
                                           ContestRepository contestRepository) {
        this.contestProblemRepository = contestProblemRepository;
        this.contestRepository = contestRepository;
    }

    /**
     * Consultar los problemas de un concurso en su orden de presentación.
     *
     * @param contestId Identificador del concurso.
     * @return Lista de resultados.
     * @throws ContestNotFoundException Si el concurso no existe.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ContestProblem> execute(UUID contestId) {
        if (contestRepository.findById(contestId).isEmpty()) {
            throw new ContestNotFoundException(contestId);
        }
        return contestProblemRepository.findAllByContestIdOrderByOrderIndexAsc(contestId);
    }
}
