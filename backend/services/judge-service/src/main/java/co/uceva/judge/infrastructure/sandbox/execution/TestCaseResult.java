package co.uceva.judge.infrastructure.sandbox.execution;

import co.uceva.shared.domain.VerdictStatus;

/**
 * Resultado de ejecutar un único caso de prueba dentro del sandbox.
 *
 * @param status       Veredicto de salida temprana ({@link VerdictStatus#MEMORY_LIMIT_EXCEEDED},
 *                     {@link VerdictStatus#TIME_LIMIT_EXCEEDED} o {@link VerdictStatus#RUNTIME_ERROR}
 *                     —este último también cuando se excede el tamaño máximo de salida—), o
 *                     {@code null} si el proceso terminó dentro de los límites y corresponde
 *                     comparar {@code output} con la salida esperada.
 * @param cpuTimeUsec  Tiempo de CPU consumido por el proceso, en microsegundos.
 * @param memoryUsedKb Pico de memoria usado por el proceso, en kilobytes.
 * @param output       Salida estándar producida por el proceso.
 */
public record TestCaseResult(VerdictStatus status, long cpuTimeUsec, long memoryUsedKb, String output) {
}
