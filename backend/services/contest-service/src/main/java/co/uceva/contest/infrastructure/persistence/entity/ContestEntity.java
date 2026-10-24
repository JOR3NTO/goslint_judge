package co.uceva.contest.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import co.uceva.contest.domain.model.ContestStatus;
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
 * Entidad JPA que representa la tabla de concursos en la base de datos.
 * Es la representación persistente de la entidad de dominio {@link co.uceva.contest.domain.model.Contest}.
 */
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "contests")
public class ContestEntity {
    /** Identificador único del concurso. */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Identificador del usuario creador del concurso. */
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    /** Título del concurso. */
    @Column(name = "title", nullable = false)
    private String title;

    /** Descripción del concurso. */
    @Column(name = "description", nullable = false)
    private String description;

    /** Contraseña cifrada de inscripción. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /** Fecha y hora de inicio del concurso. */
    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    /** Fecha y hora de finalización del concurso. */
    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    /** Estado del ciclo de vida del concurso. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ContestStatus status;

    /** Fecha y hora de creación del concurso. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
