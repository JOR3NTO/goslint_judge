package co.uceva.problem.domain.model;

import java.time.Instant;
import java.util.UUID;

import co.uceva.problem.domain.valueobject.Difficulty;
import co.uceva.problem.domain.valueobject.MemoryLimit;
import co.uceva.problem.domain.valueobject.TimeLimit;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad principal de Dominio que representa un Problema de programación
 * dentro de la plataforma. Es una clase pura de Java sin dependencias de
 * frameworks o bases de datos.
 */
@Getter
@Setter
@NoArgsConstructor
public class Problem {
    /** Identificador único universal del problema. */
    private UUID id;
    /** Identificador del usuario creador del problema. */
    private UUID createdBy;
    /** Título descriptivo del problema. */
    private String title;
    /** Enunciado completo del problema. */
    private String statement;
    /** Límite de tiempo permitido para las soluciones. */
    private TimeLimit timeLimitMs;
    /** Límite de memoria permitido para las soluciones. */
    private MemoryLimit memoryLimitKb;
    /** Nivel de dificultad del problema. */
    private Difficulty difficult;
    /** Fecha y hora de creación del problema. */
    private Instant createdAt;
    /** Descripción del formato de entrada esperado. */
    private String inputFormat;
    /** Descripción del formato de salida esperado. */
    private String outputFormat;

    /**
     * Constructor privado usado por Lombok Builder.
     * Los value objects se construyen a partir de tipos primitivos para
     * garantizar la validación de sus invariantes de dominio.
     */
    @Builder
    private Problem(UUID id, UUID createdBy, String title, String statement, int timeLimitMs, int memoryLimitKb,
            int difficult, Instant createdAt, String inputFormat, String outputFormat) {
        this.id = id;
        this.createdBy = createdBy;
        this.title = title;
        this.statement = statement;
        this.timeLimitMs = new TimeLimit(timeLimitMs);
        this.memoryLimitKb = new MemoryLimit(memoryLimitKb);
        this.difficult = new Difficulty(difficult);
        this.createdAt = createdAt;
        this.inputFormat = inputFormat;
        this.outputFormat = outputFormat;
    }

    /**
     * Factory method para crear un nuevo problema con valores por defecto.
     * Agrupa la lógica de inicialización y asigna un identificador único.
     *
     * @param createdBy     Identificador del usuario creador.
     * @param title         Título del problema.
     * @param statement     Enunciado del problema.
     * @param timeLimitMs   Límite de tiempo en milisegundos.
     * @param memoryLimitKb Límite de memoria en kilobytes.
     * @param difficult     Nivel de dificultad del problema.
     * @param inputFormat   Formato de entrada esperado.
     * @param outputFormat  Formato de salida esperado.
     * @return Una instancia de {@link Problem} lista para ser persistida.
     */
    public static Problem create(UUID createdBy, String title, String statement, int timeLimitMs, int memoryLimitKb,
            int difficult, String inputFormat, String outputFormat) {
        return Problem.builder()
                .id(UUID.randomUUID()) // Genera un identificador único seguro
                .createdBy(createdBy)
                .title(title)
                .statement(statement)
                .timeLimitMs(timeLimitMs)
                .memoryLimitKb(memoryLimitKb)
                .difficult(difficult)
                .createdAt(Instant.now()) // Fecha y hora actual de creación
                .inputFormat(inputFormat)
                .outputFormat(outputFormat)
                .build();
    }

    /**
     * Actualiza los atributos editables del problema.
     *
     * @param title         Nuevo título.
     * @param statement     Nuevo enunciado.
     * @param timeLimitMs   Nuevo límite de tiempo.
     * @param memoryLimitKb Nuevo límite de memoria.
     * @param difficult     Nueva dificultad.
     * @param inputFormat   Nuevo formato de entrada.
     * @param outputFormat  Nuevo formato de salida.
     */
    public void update(String title, String statement, TimeLimit timeLimitMs, MemoryLimit memoryLimitKb,
            Difficulty difficult, String inputFormat, String outputFormat) {
        this.title = title;
        this.statement = statement;
        this.timeLimitMs = timeLimitMs;
        this.memoryLimitKb = memoryLimitKb;
        this.difficult = difficult;
        this.inputFormat = inputFormat;
        this.outputFormat = outputFormat;
    }

    /** @return El límite de tiempo en milisegundos como entero primitivo. */
    public int getTimeLimitMs() {
        return timeLimitMs.milliseconds();
    }

    /** @param timeLimitMs Nuevo límite de tiempo en milisegundos. */
    public void setTimeLimitMs(int timeLimitMs) {
        this.timeLimitMs = new TimeLimit(timeLimitMs);
    }

    /** @return El límite de memoria en kilobytes como entero primitivo. */
    public int getMemoryLimitKb() {
        return memoryLimitKb.kilobytes();
    }

    /** @param memoryLimitKb Nuevo límite de memoria en kilobytes. */
    public void setMemoryLimitKb(int memoryLimitKb) {
        this.memoryLimitKb = new MemoryLimit(memoryLimitKb);
    }

    /** @return El nivel de dificultad como entero primitivo. */
    public int getDifficult() {
        return difficult.difficult();
    }

    /** @param difficult Nueva dificultad del problema. */
    public void setDifficult(int difficult) {
        this.difficult = new Difficulty(difficult);
    }
}
