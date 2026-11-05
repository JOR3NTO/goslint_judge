package co.uceva.problem.application.usecase.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.problem.application.usecase.GetTestCaseByIdUseCase;
import co.uceva.problem.domain.exception.TestCaseNotFoundException;
import co.uceva.problem.domain.model.TestCase;
import co.uceva.problem.domain.repository.TestCaseRepository;

/**
 * Servicio de Aplicación que implementa la consulta de un caso de prueba por identificador.
 */
@Service
public class GetTestCaseByIdUseCaseImpl implements GetTestCaseByIdUseCase{

    private final TestCaseRepository testCaseRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param testCaseRepository Puerto de salida para consultar casos de prueba.
     */
    public GetTestCaseByIdUseCaseImpl(TestCaseRepository testCaseRepository){
        this.testCaseRepository = testCaseRepository;
    }

    /**
     * Busca un caso de prueba por su identificador y lanza una excepción si no existe.
     *
     * @param TestCaseId Identificador del caso de prueba.
     * @return El caso de prueba encontrado.
     * @throws TestCaseNotFoundException Si el caso de prueba no existe.
     */
    @Override
    @Transactional
    public TestCase execute(UUID TestCaseId){
        // Buscar el caso de prueba o lanzar excepción de dominio
        TestCase testCase = testCaseRepository.findById(TestCaseId).
        orElseThrow(() -> new TestCaseNotFoundException(TestCaseId));
        return testCase;
    }
}
