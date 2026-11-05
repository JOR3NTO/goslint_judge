package co.uceva.submission.infrastructure.persistence.entity;

import co.uceva.shared.domain.ProgrammingLanguage;
import co.uceva.shared.domain.SubmissionStatus;
import co.uceva.shared.domain.VerdictStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA que representa la tabla de envíos ({@code submissions}) en la base de datos.
 * Es la representación persistente de la entidad de dominio
 * {@link co.uceva.submission.domain.model.Submission}.
 */
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "submissions")
public class SubmissionEntity {

    /** Identificador único del envío. */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Identificador del equipo que realizó el envío. */
    @Column(name = "team_id", nullable = false, updatable = false)
    private UUID teamId;

    /** Identificador del problema al que responde el envío. */
    @Column(name = "problem_id", nullable = false, updatable = false)
    private UUID problemId;

    /** Lenguaje de programación del código fuente. */
    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false, updatable = false)
    private ProgrammingLanguage language;

    /** Código fuente enviado por el estudiante. */
    @Column(name = "source_code", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String sourceCode;

    /** Veredicto actual de la evaluación. */
    @Enumerated(EnumType.STRING)
    @Column(name = "verdict", nullable = false)
    private VerdictStatus verdict;

    /** Estado del envío dentro del flujo de evaluación. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubmissionStatus status;

    /** Tiempo de ejecución en milisegundos reportado por el juez. */
    @Column(name = "execution_time_ms", nullable = false)
    private int executionTimeMs;

    /** Memoria utilizada en kilobytes reportada por el juez. */
    @Column(name = "memory_used_kb", nullable = false)
    private int memoryUsedKb;

    /** Tamaño del código fuente en bytes. */
    @Column(name = "code_size_bytes", nullable = false)
    private long codeSizeBytes;

    /** Fecha y hora exacta en la que se recibió el envío. */
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;
}
