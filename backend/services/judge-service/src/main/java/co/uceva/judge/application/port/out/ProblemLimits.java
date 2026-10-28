package co.uceva.judge.application.port.out;

import co.uceva.judge.domain.valueobject.MemoryLimit;
import co.uceva.judge.domain.valueobject.TimeLimit;

/**
 * Límites de ejecución configurados para un problema, tal como los expone
 * {@code problem-service}.
 *
 * @param timeLimit   Límite de tiempo por caso de prueba.
 * @param memoryLimit Límite de memoria por caso de prueba.
 */
public record ProblemLimits(TimeLimit timeLimit, MemoryLimit memoryLimit) {}
