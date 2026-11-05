package co.uceva.problem.application.usecase.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.problem.application.usecase.GetAllSampleTestCasesByProblemIdUseCase;
import co.uceva.problem.domain.model.TestCase;
import co.uceva.problem.domain.repository.TestCaseRepository;

/**
 * Servicio de aplicación que implementa la consulta de casos de prueba
 * de ejemplo ({@code isSample = true}) asociados a un problema.
 */
@Service
public class GetAllSampleTestCasesByProblemIdUseCaseImpl implements GetAllSampleTestCasesByProblemIdUseCase {

    private final TestCaseRepository testCaseRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param testCaseRepository Puerto de salida para consultar casos de prueba.
     */
    public GetAllSampleTestCasesByProblemIdUseCaseImpl(TestCaseRepository testCaseRepository) {
        this.testCaseRepository = testCaseRepository;
    }

    /**
     * Recupera los casos de prueba de ejemplo de un problema.
     *
     * @param problemId Identificador del problema.
     * @return Lista de casos de prueba de ejemplo.
     */
    @Override
    @Transactional(readOnly = true)
    public List<TestCase> execute(UUID problemId) {
        return testCaseRepository.findAllSampleByProblemId(problemId);
    }
}
