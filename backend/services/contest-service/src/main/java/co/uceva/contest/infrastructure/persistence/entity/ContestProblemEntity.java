package co.uceva.contest.infrastructure.persistence.entity;

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
 * Entidad JPA que representa la tabla de problemas asignados a un concurso.
 * Es la representación persistente de {@link co.uceva.contest.domain.model.ContestProblem}.
 */
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "contest_problems")
public class ContestProblemEntity {
    /** Identificador único de la asignación. */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Identificador del concurso. */
    @Column(name = "contest_id", nullable = false, updatable = false)
    private UUID contestId;

    /** Identificador del problema en problem-service. */
    @Column(name = "problem_id", nullable = false, updatable = false)
    private UUID problemId;

    /** Alias visible del problema dentro del concurso. */
    @Column(name = "alias", nullable = false)
    private String alias;

    /** Posición del problema en el concurso. */
    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    /** Puntos que otorga resolver el problema. */
    @Column(name = "points", nullable = false)
    private int points;

    /** Minutos de penalización por intento fallido. */
    @Column(name = "penalty", nullable = false)
    private int penalty;

    /** Color del globo en formato {@code #RRGGBB}. */
    @Column(name = "balloon_color", nullable = false)
    private String balloonColor;
}
