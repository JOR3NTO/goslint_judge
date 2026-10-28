package co.uceva.contest.application.usecase.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contest.application.usecase.RemoveProblemFromContestUseCase;
import co.uceva.contest.domain.exception.ContestProblemNotFoundException;
import co.uceva.contest.domain.repository.ContestProblemRepository;

/**
 * Servicio de Aplicación que implementa el caso de uso de retiro de un problema de un concurso.
 */
@Service
public class RemoveProblemFromContestUseCaseImpl implements RemoveProblemFromContestUseCase {

    private final ContestProblemRepository contestProblemRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param contestProblemRepository Puerto de salida para gestionar problemas de concurso.
     */
    public RemoveProblemFromContestUseCaseImpl(ContestProblemRepository contestProblemRepository) {
        this.contestProblemRepository = contestProblemRepository;
    }

    /**
     * Retira un problema de un concurso verificando previamente su existencia.
     *
     * @param contestProblemId Identificador del problema de concurso a retirar.
     * @throws ContestProblemNotFoundException Si la asignación no existe.
     */
    @Override
    @Transactional
    public void execute(UUID contestProblemId) {
        if (contestProblemRepository.findById(contestProblemId).isEmpty()) {
            throw new ContestProblemNotFoundException(contestProblemId);
        }
        contestProblemRepository.deleteById(contestProblemId);
    }
}
