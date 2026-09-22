package co.uceva.problem.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

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
 * Entidad JPA que representa la tabla de casos de prueba en la base de datos.
 * Es la representación persistente de la entidad de dominio {@link co.uceva.problem.domain.model.TestCase}.
 */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "test_cases")
public class TestCaseEntity {
    /** Identificador único del caso de prueba. */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Identificador del problema al que pertenece el caso de prueba. */
    @Column(name = "problem_id", nullable = false, updatable = false)
    private UUID problemId;

    /** Salida esperada para la entrada del caso. */
    @Column(name = "expected_output", nullable = false, updatable = true)
    private String expectedOutput;

    /** Índice que define el orden de ejecución del caso. */
    @Column(name = "order_index", nullable = false, updatable = true)
    private int orderIndex;

    /** Indica si el caso de prueba es público (de ejemplo). */
    @Column(name = "is_sample", nullable = false, updatable = true)
    private boolean isSample;

    /** Entrada del caso de prueba. */
    @Column(name = "input", nullable = false, updatable = true)
    private String input;

    /** Salida esperada del caso de prueba. */
    @Column(name = "output", nullable = false, updatable = true)
    private String output;

    /** Fecha y hora de creación del caso de prueba. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
