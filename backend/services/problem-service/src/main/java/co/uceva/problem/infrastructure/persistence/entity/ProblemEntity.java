package co.uceva.problem.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import co.uceva.problem.domain.valueobject.Difficulty;
import co.uceva.problem.domain.valueobject.MemoryLimit;
import co.uceva.problem.domain.valueobject.TimeLimit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad JPA que representa la tabla de problemas en la base de datos.
 * Es la representación persistente de la entidad de dominio {@link co.uceva.problem.domain.model.Problem}.
 */
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "problems")
public class ProblemEntity {
    /** Identificador único del problema. */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Identificador del usuario creador del problema. */
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    /** Título del problema. */
    @Column(name = "title", nullable = false, updatable = true)
    private String title;

    /** Enunciado del problema. */
    @Column(name = "statement", nullable = false, updatable = true)
    private String statement;

    /** Límite de tiempo permitido en milisegundos. */
    @Column(name = "time_limit_ms", nullable = false, updatable = true)
    private int timeLimitMs;

    /** Límite de memoria permitido en kilobytes. */
    @Column(name = "memory_limit_kb", nullable = false, updatable = true)
    private int memoryLimitKb;

    /** Nivel de dificultad del problema. */
    @Column(name = "difficulty", nullable = false, updatable = true)
    private int difficult;

    /** Fecha y hora de creación del problema. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Descripción del formato de entrada esperado. */
    @Column(name = "input_format", nullable = false, updatable = true)
    private String inputFormat;

    /** Descripción del formato de salida esperado. */
    @Column(name = "output_format", nullable = false, updatable = true)
    private String outputFormat;
}
