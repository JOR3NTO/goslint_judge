package co.uceva.submission.domain.model;

import co.uceva.shared.domain.ProgrammingLanguage;
import co.uceva.shared.domain.VerdictStatus;
import co.uceva.submission.domain.valueobject.SourceCode;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad principal de Dominio que representa un envío de código fuente
 * realizado por un equipo de estudiantes para resolver un problema.
 * <p>
 * Esta clase es pura de Java y no tiene dependencias de Spring Boot,
 * bases de datos ni frameworks externos.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
public class Submission {

    /** Identificador único universal del envío. */
    private UUID id;
    /** Identificador del equipo que realizó el envío. */
    private UUID teamId;
    /** Identificador del problema al que responde el envío. */
    private UUID problemId;
    /** Lenguaje de programación en el que está escrito el código. */
    private ProgrammingLanguage language;
    /** Código fuente enviado por el estudiante. */
    private SourceCode sourceCode;
    /** Veredicto actual de la evaluación. */
    private VerdictStatus verdict;
    /** Tiempo de ejecución en milisegundos reportado por el juez. */
    private int executionTimeMs;
    /** Memoria utilizada en kilobytes reportada por el juez. */
    private int memoryUsedKb;
    /** Tamaño del código fuente en bytes. */
    private long codeSizeBytes;
    /** Fecha y hora exacta en la que se recibió el envío. */
    private Instant submittedAt;

    /**
     * Constructor privado usado por Lombok Builder.
     * El value object {@link SourceCode} se construye a partir de los datos
     * primitivos para garantizar la validación de sus invariantes de dominio.
     *
     * @param id              Identificador del envío.
     * @param teamId          Identificador del equipo.
     * @param problemId       Identificador del problema.
     * @param language        Lenguaje de programación.
     * @param sourceCode      Código fuente en texto plano.
     * @param verdict         Veredicto de evaluación.
     * @param executionTimeMs Tiempo de ejecución en milisegundos.
     * @param memoryUsedKb    Memoria utilizada en kilobytes.
     * @param submittedAt     Fecha de recepción del envío.
     */
    @Builder
    private Submission(UUID id, UUID teamId, UUID problemId, ProgrammingLanguage language, String sourceCode,
            VerdictStatus verdict, int executionTimeMs, int memoryUsedKb, Instant submittedAt) {
        this.id = id;
        this.teamId = teamId;
        this.problemId = problemId;
        this.language = language;
        this.sourceCode = new SourceCode(sourceCode, language);
        this.verdict = verdict;
        this.executionTimeMs = executionTimeMs;
        this.memoryUsedKb = memoryUsedKb;
        this.codeSizeBytes = this.sourceCode.sizeInBytes();
        this.submittedAt = submittedAt;
    }

    /**
     * Factory method para crear un nuevo envío con valores por defecto.
     * Agrupa la lógica de inicialización y asigna un identificador único,
     * un veredicto inicial {@code PENDING} y la fecha actual.
     *
     * @param teamId     Identificador del equipo que envía la solución.
     * @param problemId  Identificador del problema a resolver.
     * @param language   Lenguaje de programación del código fuente.
     * @param sourceCode Código fuente en texto plano.
     * @return Una instancia de {@link Submission} lista para ser persistida.
     */
    public static Submission create(UUID teamId, UUID problemId, ProgrammingLanguage language, String sourceCode) {
        return Submission.builder()
                .id(UUID.randomUUID())
                .teamId(teamId)
                .problemId(problemId)
                .language(language)
                .sourceCode(sourceCode)
                .verdict(VerdictStatus.PENDING)
                .executionTimeMs(0)
                .memoryUsedKb(0)
                .submittedAt(Instant.now())
                .build();
    }

    /**
     * Actualiza el veredicto del envío una vez que el juez ha completado
     * la evaluación.
     *
     * @param verdict         Nuevo veredicto emitido.
     * @param executionTimeMs Tiempo de ejecución medido en milisegundos.
     * @param memoryUsedKb    Memoria utilizada medida en kilobytes.
     */
    public void updateVerdict(VerdictStatus verdict, int executionTimeMs, int memoryUsedKb) {
        this.verdict = verdict;
        this.executionTimeMs = executionTimeMs;
        this.memoryUsedKb = memoryUsedKb;
    }

    /** @return El código fuente en texto plano. */
    public String getSourceCode() {
        return sourceCode.content();
    }

    /** @param sourceCode Nuevo código fuente en texto plano. */
    public void setSourceCode(String sourceCode) {
        this.sourceCode = new SourceCode(sourceCode, this.language);
        this.codeSizeBytes = this.sourceCode.sizeInBytes();
    }
}
