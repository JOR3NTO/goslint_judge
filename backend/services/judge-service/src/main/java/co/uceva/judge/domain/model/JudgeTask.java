package co.uceva.judge.domain.model;

import java.util.List;
import java.util.UUID;

import co.uceva.judge.domain.valueobject.MemoryLimit;
import co.uceva.judge.domain.valueobject.TimeLimit;
import co.uceva.shared.domain.ProgrammingLanguage;
import co.uceva.shared.domain.valueobject.SourceCode;
import lombok.Builder;
import lombok.Getter;

/**
 * Entidad de Dominio que representa una tarea de evaluación: todo lo que
 * {@code judge-service} necesita para ejecutar, dentro del sandbox, la
 * solución de un envío contra los casos de prueba de su problema.
 * <p>
 * Se ensambla a partir del {@code SubmissionReceivedEvent} publicado por
 * {@code submission-service} y de los casos de prueba obtenidos desde
 * {@code problem-service}. Esta clase es pura de Java y no tiene dependencias
 * de Spring Boot, bases de datos ni frameworks externos.
 * </p>
 */
@Getter
public class JudgeTask {

    /** Identificador único del envío que se va a evaluar. */
    private final UUID submissionId;
    /** Lenguaje de programación en el que está escrito el código. */
    private final ProgrammingLanguage language;
    /** Código fuente enviado por el estudiante. */
    private final SourceCode sourceCode;
    /** Casos de prueba del problema, en el orden en que deben ejecutarse. */
    private final List<TestCase> testCases;
    /** Límite de tiempo configurado para el problema. */
    private final TimeLimit timeLimit;
    /** Límite de memoria configurado para el problema. */
    private final MemoryLimit memoryLimit;

    /**
     * Constructor privado usado por Lombok Builder.
     * Los value objects {@link SourceCode}, {@link TimeLimit} y
     * {@link MemoryLimit} se construyen a partir de los datos primitivos para
     * garantizar la validación de sus invariantes de dominio.
     *
     * @param submissionId  Identificador del envío a evaluar.
     * @param language      Lenguaje de programación del código fuente.
     * @param sourceCode    Código fuente en texto plano.
     * @param testCases     Casos de prueba del problema.
     * @param timeLimitMs   Límite de tiempo, en milisegundos, del problema.
     * @param memoryLimitKb Límite de memoria, en kilobytes, del problema.
     */
    @Builder
    private JudgeTask(UUID submissionId, ProgrammingLanguage language, String sourceCode, List<TestCase> testCases,
            int timeLimitMs, int memoryLimitKb) {
        if (submissionId == null) {
            throw new IllegalArgumentException("El identificador del envío es obligatorio.");
        }
        if (testCases == null || testCases.isEmpty()) {
            throw new IllegalArgumentException("Una tarea de evaluación debe tener al menos un caso de prueba.");
        }
        this.submissionId = submissionId;
        this.language = language;
        this.sourceCode = new SourceCode(sourceCode, language);
        this.testCases = List.copyOf(testCases);
        this.timeLimit = new TimeLimit(timeLimitMs);
        this.memoryLimit = new MemoryLimit(memoryLimitKb);
    }

    /**
     * Factory method para ensamblar una nueva tarea de evaluación.
     *
     * @param submissionId  Identificador del envío a evaluar.
     * @param language      Lenguaje de programación del código fuente.
     * @param sourceCode    Código fuente en texto plano.
     * @param testCases     Casos de prueba del problema, en orden de ejecución.
     * @param timeLimitMs   Límite de tiempo, en milisegundos, del problema.
     * @param memoryLimitKb Límite de memoria, en kilobytes, del problema.
     * @return Una instancia de {@link JudgeTask} lista para ser ejecutada en el sandbox.
     */
    public static JudgeTask create(UUID submissionId, ProgrammingLanguage language, String sourceCode,
            List<TestCase> testCases, int timeLimitMs, int memoryLimitKb) {
        return JudgeTask.builder()
                .submissionId(submissionId)
                .language(language)
                .sourceCode(sourceCode)
                .testCases(testCases)
                .timeLimitMs(timeLimitMs)
                .memoryLimitKb(memoryLimitKb)
                .build();
    }
}
