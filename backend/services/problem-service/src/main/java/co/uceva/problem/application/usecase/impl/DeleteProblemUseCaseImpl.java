package co.uceva.problem.application.usecase.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.problem.application.usecase.DeleteProblemUseCase;
import co.uceva.problem.domain.exception.ProblemNotFoundException;
import co.uceva.problem.domain.exception.TestCaseNotFoundException;
import co.uceva.problem.domain.repository.ProblemRepository;

/**
 * Servicio de Aplicación que implementa el caso de uso de eliminación de un problema.
 */
@Service
public class DeleteProblemUseCaseImpl implements DeleteProblemUseCase{

    private final ProblemRepository problemRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param problemRepository Puerto de salida para gestionar problemas.
     */
    public DeleteProblemUseCaseImpl(ProblemRepository problemRepository){
        this.problemRepository = problemRepository;
    }

    /**
     * Ejecuta la eliminación de un problema verificando previamente su existencia.
     *
     * @param problemId Identificador del problema a eliminar.
     * @throws ProblemNotFoundException Si el problema no existe.
     */
    @Override
    @Transactional
    public void execute(UUID problemId){
        // Verificar que el problema exista antes de eliminarlo
        if (!problemRepository.findById(problemId).isPresent()) {
            throw new ProblemNotFoundException(problemId);
        }
        // Eliminar el problema
        problemRepository.deleteById(problemId);
    }
}
