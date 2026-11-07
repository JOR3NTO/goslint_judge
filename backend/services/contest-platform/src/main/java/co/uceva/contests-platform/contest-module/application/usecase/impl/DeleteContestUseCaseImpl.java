package co.uceva.contestsplatform.contestmodule.application.usecase.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contestsplatform.contestmodule.application.usecase.DeleteContestUseCase;
import co.uceva.contestsplatform.contestmodule.domain.exception.ContestNotFoundException;
import co.uceva.contestsplatform.contestmodule.domain.repository.ContestRepository;

/**
 * Servicio de Aplicación que implementa el caso de uso de eliminación de un concurso.
 */
@Service
public class DeleteContestUseCaseImpl implements DeleteContestUseCase {

    private final ContestRepository contestRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param contestRepository Puerto de salida para gestionar concursos.
     */
    public DeleteContestUseCaseImpl(ContestRepository contestRepository) {
        this.contestRepository = contestRepository;
    }

    /**
     * Ejecuta la eliminación de un concurso verificando previamente su existencia.
     *
     * @param contestId Identificador del concurso a eliminar.
     * @throws ContestNotFoundException Si el concurso no existe.
     */
    @Override
    @Transactional
    public void execute(UUID contestId) {
        if (contestRepository.findById(contestId).isEmpty()) {
            throw new ContestNotFoundException(contestId);
        }
        contestRepository.deleteById(contestId);
    }
}
