package co.uceva.contests_platform.problem.application.usecase.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contests_platform.problem.application.usecase.GetProblemByIdUseCase;
import co.uceva.contests_platform.problem.domain.exception.ProblemNotFoundException;
import co.uceva.contests_platform.problem.domain.model.Problem;
import co.uceva.contests_platform.problem.domain.repository.ProblemRepository;

/**
 * Servicio de Aplicación que implementa la consulta de un problema por identificador.
 */
@Service
public class GetProblemByIdUseCaseImpl implements GetProblemByIdUseCase{

    private final ProblemRepository problemRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param problemRepository Puerto de salida para consultar problemas.
     */
    public GetProblemByIdUseCaseImpl(ProblemRepository problemRepository){
        this.problemRepository = problemRepository;
    }

    /**
     * Busca un problema por su identificador y lanza una excepción si no existe.
     *
     * @param problemId Identificador del problema.
     * @return El problema encontrado.
     * @throws ProblemNotFoundException Si el problema no existe.
     */
    @Override
    @Transactional
    public Problem execute(UUID problemId){
        // Buscar el problema o lanzar excepción de dominio
        Problem problem = problemRepository.findById(problemId)
            .orElseThrow(() -> new ProblemNotFoundException(problemId));
        return problem;
    }
}
