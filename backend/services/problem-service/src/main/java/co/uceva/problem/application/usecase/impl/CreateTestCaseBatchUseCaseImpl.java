package co.uceva.problem.application.usecase.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.problem.application.usecase.CreateTestCaseBatchUseCase;
import co.uceva.problem.application.usecase.CreateTestCaseUseCase;
import co.uceva.problem.domain.exception.ProblemNotFoundException;
import co.uceva.problem.domain.model.TestCase;
import co.uceva.problem.domain.repository.ProblemRepository;
import co.uceva.problem.domain.repository.TestCaseRepository;

/**
 * Servicio de Aplicación que implementa la creación masiva de casos de prueba.
 * Reemplaza los casos de prueba existentes de un problema por los nuevos.
 */
@Service
public class CreateTestCaseBatchUseCaseImpl implements CreateTestCaseBatchUseCase {

    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param problemRepository  Puerto de salida para verificar la existencia del problema.
     * @param testCaseRepository Puerto de salida para gestionar casos de prueba.
     */
    public CreateTestCaseBatchUseCaseImpl(ProblemRepository problemRepository, TestCaseRepository testCaseRepository) {
        this.problemRepository = problemRepository;
        this.testCaseRepository = testCaseRepository;
    }

    /**
     * Ejecuta el flujo de creación masiva:
     * 1. Verifica que el problema exista.
     * 2. Elimina los casos de prueba actuales del problema.
     * 3. Construye los nuevos casos de prueba.
     * 4. Persiste la nueva lista.
     *
     * @param command Datos de entrada con el problema y los nuevos casos de prueba.
     * @return Lista de casos de prueba creados.
     */
    @Override
    @Transactional
    public List<TestCase> execute(CreateTestCaseBatchCommand command) {
        // Verificar que el problema asociado exista
        problemRepository.findById(command.problemId())
                .orElseThrow(() -> new ProblemNotFoundException(command.problemId()));

        // Reemplazar los casos de prueba existentes del problema
        testCaseRepository.deleteByProblemId(command.problemId());

        // Construir los nuevos casos de prueba a partir de los comandos recibidos
        List<TestCase> newTestCases = command.testCases().stream()
                .map(tc -> TestCase.create(
                        command.problemId(),
                        tc.expectedOutput(),
                        tc.orderIndex(),
                        tc.isSample(),
                        tc.input(),
                        tc.output()
                ))
                .toList();

        // Persistir todos los nuevos casos de prueba
        return testCaseRepository.saveAll(newTestCases);
    }
}
