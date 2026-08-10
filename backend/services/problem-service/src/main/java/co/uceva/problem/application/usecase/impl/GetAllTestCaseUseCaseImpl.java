package co.uceva.problem.application.usecase.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.problem.application.usecase.GetAllTestCaseByProblemIdUseCase;
import co.uceva.problem.domain.model.TestCase;
import co.uceva.problem.domain.repository.TestCaseRepository;

/**
 * Servicio de Aplicación que implementa la consulta de casos de prueba por problema.
 */
@Service
public class GetAllTestCaseUseCaseImpl implements GetAllTestCaseByProblemIdUseCase{

    private final TestCaseRepository testCaseRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param testCaseRepository Puerto de salida para consultar casos de prueba.
     */
    public GetAllTestCaseUseCaseImpl(TestCaseRepository testCaseRepository){
        this.testCaseRepository = testCaseRepository;
    }

    /**
     * Recupera todos los casos de prueba asociados a un problema.
     *
     * @param problemId Identificador del problema.
     * @return Lista de casos de prueba del problema.
     */
    @Override
    @Transactional
    public List<TestCase> execute(UUID problemId){
        return testCaseRepository.findAllByProblemId(problemId);
    }
}
