package co.uceva.problem.application.usecase.impl;

import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.problem.application.usecase.DeleteTestCaseBatchUseCase;
import co.uceva.problem.domain.model.TestCase;
import co.uceva.problem.domain.repository.TestCaseRepository;

/**
 * Servicio de Aplicación que implementa la eliminación masiva de casos de prueba.
 * Después de eliminar, reordena los casos de prueba restantes de cada problema afectado.
 */
@Service
public class DeleteTestCaseBatchUseCaseImpl implements DeleteTestCaseBatchUseCase {

    private final TestCaseRepository testCaseRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param testCaseRepository Puerto de salida para gestionar casos de prueba.
     */
    public DeleteTestCaseBatchUseCaseImpl(TestCaseRepository testCaseRepository) {
        this.testCaseRepository = testCaseRepository;
    }

    /**
     * Ejecuta la eliminación masiva:
     * 1. Filtra los identificadores que corresponden a casos de prueba existentes.
     * 2. Elimina los casos de prueba encontrados.
     * 3. Reordena los casos de prueba restantes de cada problema afectado.
     *
     * @param testCaseIds Lista de identificadores de casos de prueba a eliminar.
     */
    @Override
    @Transactional
    public void execute(List<UUID> testCaseIds) {
        // Si la lista es nula o vacía no hay nada que procesar
        if (testCaseIds == null || testCaseIds.isEmpty()) {
            return;
        }

        // Identificar problemas afectados y casos de prueba existentes
        Set<UUID> problemIdsAffected = new HashSet<>();
        List<UUID> existingIds = new ArrayList<>();

        for (UUID testCaseId : testCaseIds) {
            Optional<TestCase> testCase = testCaseRepository.findById(testCaseId);
            if (testCase.isPresent()) {
                existingIds.add(testCaseId);
                problemIdsAffected.add(testCase.get().getProblemId());
            }
        }

        // Eliminar solo los casos de prueba que existen
        if (!existingIds.isEmpty()) {
            testCaseRepository.deleteAllById(existingIds);
        }

        // Reordenar los casos de prueba restantes de cada problema afectado
        for (UUID problemId : problemIdsAffected) {
            List<TestCase> remaining = testCaseRepository.findAllByProblemId(problemId);
            if (remaining.isEmpty()) {
                continue;
            }

            Map<UUID, Integer> newOrders = new LinkedHashMap<>();
            for (int i = 0; i < remaining.size(); i++) {
                newOrders.put(remaining.get(i).getId(), i + 1);
            }
            testCaseRepository.updateOrderIndexes(newOrders);
        }
    }
}
