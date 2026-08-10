package co.uceva.problem.application.usecase.impl;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.problem.application.usecase.CreateTestCaseUseCase;
import co.uceva.problem.domain.model.TestCase;
import co.uceva.problem.domain.repository.TestCaseRepository;

/**
 * Servicio de Aplicación que implementa el caso de uso de creación de un caso de prueba.
 */
@Service
public class CreateTestCaseUseCaseImpl implements CreateTestCaseUseCase {

    private final TestCaseRepository testCaseRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param testCaseRepository Puerto de salida para persistir casos de prueba.
     */
    public CreateTestCaseUseCaseImpl(TestCaseRepository testCaseRepository){
        this.testCaseRepository = testCaseRepository;
    }

    /**
     * Ejecuta el flujo de creación de un caso de prueba:
     * 1. Construye la entidad de dominio a partir del comando.
     * 2. Persiste el caso de prueba.
     *
     * @param command Datos de entrada para crear el caso de prueba.
     * @return El caso de prueba creado y persistido.
     */
    @Override
    @Transactional
    public TestCase execute(CreateTestCaseCommand command){
        // Construir la entidad de dominio usando el factory method
        TestCase testCase = TestCase.create(
            command.problemId(),
            command.expectedOutput(),
            command.orderIndex(),
            command.isSample(),
            command.input(),
            command.output()
        );
        // Persistir el caso de prueba
        return testCaseRepository.save(testCase);
    }
}
