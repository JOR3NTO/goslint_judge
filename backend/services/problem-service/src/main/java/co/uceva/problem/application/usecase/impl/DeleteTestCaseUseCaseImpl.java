package co.uceva.problem.application.usecase.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.problem.application.usecase.DeleteTestCaseUseCase;
import co.uceva.problem.domain.exception.TestCaseNotFoundException;
import co.uceva.problem.domain.repository.TestCaseRepository;

/**
 * Servicio de Aplicación que implementa el caso de uso de eliminación de un caso de prueba.
 */
@Service
public class DeleteTestCaseUseCaseImpl implements DeleteTestCaseUseCase{

    private final TestCaseRepository testCaseRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param testCaseRepository Puerto de salida para gestionar casos de prueba.
     */
    public DeleteTestCaseUseCaseImpl(TestCaseRepository testCaseRepository){
        this.testCaseRepository = testCaseRepository;
    }

    /**
     * Ejecuta la eliminación de un caso de prueba verificando previamente su existencia.
     *
     * @param testCaseId Identificador del caso de prueba a eliminar.
     * @throws TestCaseNotFoundException Si el caso de prueba no existe.
     */
    @Override
    @Transactional
    public void execute(UUID testCaseId){
        // Verificar que el caso de prueba exista antes de eliminarlo
        if (!testCaseRepository.findById(testCaseId).isPresent()) {
            throw new TestCaseNotFoundException(testCaseId);
        }
        // Eliminar el caso de prueba
        testCaseRepository.deleteById(testCaseId);
    }
}
