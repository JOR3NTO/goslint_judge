package co.uceva.problem.application.usecase.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.problem.application.usecase.GetAllProblemsByTitleUseCase;
import co.uceva.problem.domain.model.Problem;
import co.uceva.problem.domain.repository.ProblemRepository;

/**
 * Servicio de Aplicación que implementa la búsqueda de problemas por título.
 */
@Service
public class GetAllProblemsByTitleUseCaseImpl implements GetAllProblemsByTitleUseCase{

    private final ProblemRepository problemRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param problemRepository Puerto de salida para consultar problemas.
     */
    public GetAllProblemsByTitleUseCaseImpl(ProblemRepository problemRepository){
        this.problemRepository = problemRepository;
    }

    /**
     * Recupera los problemas cuyo título coincida con el criterio dado.
     *
     * @param title Título o fragmento a buscar.
     * @return Lista de problemas que coinciden.
     */
    @Override
    @Transactional
    public List<Problem> execute(String title){
        return problemRepository.findAllByTitle(title);
    }
}
