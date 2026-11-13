package co.uceva.contests_platform.problem.application.usecase.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contests_platform.problem.application.usecase.GetAllProblemsUseCase;
import co.uceva.contests_platform.problem.domain.model.Problem;
import co.uceva.contests_platform.problem.domain.repository.ProblemRepository;

/**
 * Servicio de Aplicación que implementa la consulta de todos los problemas.
 */
@Service
public class GetAllProblemsUseCaseImpl implements GetAllProblemsUseCase{

    private final ProblemRepository problemRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param problemRepository Puerto de salida para consultar problemas.
     */
    public GetAllProblemsUseCaseImpl(ProblemRepository problemRepository){
        this.problemRepository = problemRepository;
    }

    /**
     * Recupera todos los problemas registrados en el sistema.
     *
     * @return Lista completa de problemas.
     */
    @Override
    @Transactional
    public List<Problem> execute(){
        return problemRepository.findAll();
    }
}
