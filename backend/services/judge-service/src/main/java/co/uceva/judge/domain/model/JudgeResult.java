package co.uceva.judge.domain.model;

import java.util.UUID;

import co.uceva.shared.domain.VerdictStatus;
import lombok.Getter;

/**
 * Entidad de Dominio que representa el resultado de evaluar, dentro del
 * sandbox, la solución de un envío contra los casos de prueba de su problema.
 * <p>
 * Es la contrapartida de {@link JudgeTask}: mientras aquella describe lo que
 * hay que ejecutar, esta describe lo que se obtuvo al hacerlo. Se traduce
 * directamente al {@code SubmissionJudgedEvent} que {@code judge-service}
 * publica al terminar de evaluar. Esta clase es pura de Java y no tiene
 * dependencias de Spring Boot, bases de datos ni frameworks externos.
 * </p>
 */
@Getter
public class JudgeResult {

    /** Identificador único del envío evaluado. */
    private final UUID submissionId;
    /** Veredicto final emitido por el motor de evaluación. */
    private final VerdictStatus verdict;
    /** Tiempo de ejecución máximo observado, en milisegundos. */
    private final int executionTimeMs;
    /** Memoria máxima utilizada, en kilobytes, observada durante la ejecución. */
    private final int memoryUsedKb;
    /**
     * Identificador del caso de prueba en el que falló la solución, o
     * {@code null} si el veredicto fue {@link VerdictStatus#ACCEPTED} o si el
     * fallo ocurrió antes de ejecutar cualquier caso de prueba (por ejemplo,
     * un {@link VerdictStatus#COMPILATION_ERROR}).
     */
    private final UUID failedTestCase;

    private JudgeResult(UUID submissionId, VerdictStatus verdict, int executionTimeMs, int memoryUsedKb,
            UUID failedTestCase) {
        if (submissionId == null) {
            throw new IllegalArgumentException("El identificador del envío es obligatorio.");
        }
        if (verdict == null) {
            throw new IllegalArgumentException("El veredicto de la evaluación es obligatorio.");
        }
        if (executionTimeMs < 0) {
            throw new IllegalArgumentException("El tiempo de ejecución no puede ser negativo.");
        }
        if (memoryUsedKb < 0) {
            throw new IllegalArgumentException("La memoria utilizada no puede ser negativa.");
        }
        if (verdict == VerdictStatus.ACCEPTED && failedTestCase != null) {
            throw new IllegalArgumentException("Un resultado aceptado no puede tener un caso de prueba fallido.");
        }
        this.submissionId = submissionId;
        this.verdict = verdict;
        this.executionTimeMs = executionTimeMs;
        this.memoryUsedKb = memoryUsedKb;
        this.failedTestCase = failedTestCase;
    }

    /**
     * Factory method para ensamblar el resultado de una evaluación a partir de
     * las métricas y el veredicto obtenidos al ejecutar la solución en el
     * sandbox.
     *
     * @param submissionId    Identificador del envío evaluado.
     * @param verdict         Veredicto emitido por el motor de evaluación.
     * @param executionTimeMs Tiempo de ejecución máximo observado, en milisegundos.
     * @param memoryUsedKb    Memoria máxima utilizada, en kilobytes.
     * @param failedTestCase  Identificador del caso de prueba que falló, o {@code null} si el
     *                        veredicto fue {@link VerdictStatus#ACCEPTED} o si no aplica.
     * @return Un {@link JudgeResult} con el veredicto y las métricas indicadas.
     */
    public static JudgeResult create(UUID submissionId, VerdictStatus verdict, int executionTimeMs,
            int memoryUsedKb, UUID failedTestCase) {
        return new JudgeResult(submissionId, verdict, executionTimeMs, memoryUsedKb, failedTestCase);
    }
}
