package co.uceva.contest.infrastructure.persistence.entity;

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
 * Entidad JPA que representa la tabla de integrantes de equipo.
 * Es la representación persistente de {@link co.uceva.contest.domain.model.Member}.
 */
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "team_members")
public class MemberEntity {
    /** Identificador único de la membresía. */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Identificador del equipo al que pertenece el integrante. */
    @Column(name = "team_id", nullable = false, updatable = false)
    private UUID teamId;

    /** Identificador del usuario integrante. */
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    /** Fecha y hora en la que el integrante se unió al equipo. */
    @Column(name = "registered_at", nullable = false, updatable = false)
    private Instant registeredAt;
}
