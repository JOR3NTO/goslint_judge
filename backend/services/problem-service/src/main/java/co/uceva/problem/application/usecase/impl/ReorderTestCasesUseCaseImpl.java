package co.uceva.problem.application.usecase.impl;

import co.uceva.problem.application.usecase.ReorderTestCasesUseCase;
import co.uceva.problem.domain.exception.ProblemNotFoundException;
import co.uceva.problem.domain.model.TestCase;
import co.uceva.problem.domain.repository.ProblemRepository;
import co.uceva.problem.domain.repository.TestCaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Servicio de Aplicación que implementa el reordenamiento de casos de prueba.
 */
@Service
public class ReorderTestCasesUseCaseImpl implements ReorderTestCasesUseCase {

    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param problemRepository  Puerto de salida para verificar problemas.
     * @param testCaseRepository Puerto de salida para gestionar casos de prueba.
     */
    public ReorderTestCasesUseCaseImpl(ProblemRepository problemRepository, TestCaseRepository testCaseRepository) {
        this.problemRepository = problemRepository;
        this.testCaseRepository = testCaseRepository;
    }

    /**
     * Ejecuta el reordenamiento:
     * 1. Verifica que el problema exista.
     * 2. Valida que la cantidad de IDs coincida con los casos actuales.
     * 3. Construye un mapa con los nuevos índices de orden.
     * 4. Persiste los cambios.
     *
     * @param problemId          Identificador del problema.
     * @param orderedTestCaseIds Lista de identificadores en el nuevo orden.
     * @throws ProblemNotFoundException Si el problema no existe.
     * @throws RuntimeException         Si la cantidad de IDs no coincide.
     */
    @Override
    @Transactional
    public void execute(UUID problemId, List<UUID> orderedTestCaseIds) {
        // Verificar que el problema exista
        problemRepository.findById(problemId)
                .orElseThrow(() -> new ProblemNotFoundException(problemId));

        // Validar que la cantidad de IDs coincida con los casos actuales
        List<TestCase> currentTestCases = testCaseRepository.findAllByProblemId(problemId);
        if (currentTestCases.size() != orderedTestCaseIds.size()) {
            throw new RuntimeException(
                    "La cantidad de IDs enviados no coincide con el total de casos de prueba del problema."
            );
        }

        // Construir mapa de nuevos índices (secuencia base 1)
        Map<UUID, Integer> newOrdersMap = new HashMap<>();
        for (int i = 0; i < orderedTestCaseIds.size(); i++) {
            UUID testCaseId = orderedTestCaseIds.get(i);
            int newOrderIndex = i + 1; // Secuencia base 1 (1, 2, 3...)
            newOrdersMap.put(testCaseId, newOrderIndex);
        }

        // Persistir los nuevos índices de orden
        testCaseRepository.updateOrderIndexes(newOrdersMap);
    }
}