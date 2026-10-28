package co.uceva.contest.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import co.uceva.contest.domain.model.ClarificationStatus;
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

/**
 * Entidad JPA que representa la tabla de aclaraciones.
 * Es la representación persistente de {@link co.uceva.contest.domain.model.Clarification}.
 */
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "clarifications")
public class ClarificationEntity {
    /** Identificador único de la aclaración. */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Identificador del problema del concurso sobre el que se pregunta. */
    @Column(name = "problem_id", nullable = false, updatable = false)
    private UUID problemId;

    /** Identificador del equipo que pregunta. */
    @Column(name = "team_id", nullable = false, updatable = false)
    private UUID teamId;

    /** Pregunta del equipo. */
    @Column(name = "question", nullable = false, updatable = false, columnDefinition = "text")
    private String question;

    /** Respuesta de los organizadores; nula mientras esté pendiente. */
    @Column(name = "answer", columnDefinition = "text")
    private String answer;

    /** Estado de la aclaración. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ClarificationStatus status;

    /** Fecha y hora en que se envió la pregunta. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Fecha y hora en que se respondió; nula mientras esté pendiente. */
    @Column(name = "answered_at")
    private Instant answeredAt;
}
