package co.uceva.problem.application.usecase.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.problem.application.usecase.UpdateTestCaseUseCase;
import co.uceva.problem.domain.exception.TestCaseNotFoundException;
import co.uceva.problem.domain.model.TestCase;
import co.uceva.problem.domain.repository.TestCaseRepository;

/**
 * Servicio de Aplicación que implementa el caso de uso de actualización de un caso de prueba.
 */
@Service
public class UpdateTestCaseUseCaseImpl implements UpdateTestCaseUseCase {

    private final TestCaseRepository testCaseRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param testCaseRepository Puerto de salida para gestionar casos de prueba.
     */
    public UpdateTestCaseUseCaseImpl(TestCaseRepository testCaseRepository){
        this.testCaseRepository = testCaseRepository;
    }

    /**
     * Ejecuta la actualización de un caso de prueba:
     * 1. Busca el caso de prueba existente.
     * 2. Actualiza sus atributos.
     * 3. Persiste los cambios.
     *
     * @param command Datos de entrada para actualizar el caso de prueba.
     * @return El caso de prueba actualizado y persistido.
     * @throws TestCaseNotFoundException Si el caso de prueba no existe.
     */
    @Override
    @Transactional
    public TestCase execute(UpdateTestCaseCommand command){
        // Buscar el caso de prueba o lanzar excepción de dominio
        TestCase testCase = testCaseRepository.findById(command.testCaseId())
        .orElseThrow(() -> new TestCaseNotFoundException(command.testCaseId()));

        // Actualizar la entidad de dominio
        testCase.update(
            command.expectedOutput(),
            command.orderIndex(),
            command.isSample(),
            command.input(),
            command.output());

        // Persistir los cambios
        return testCaseRepository.save(testCase);
    }
}
