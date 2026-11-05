package co.uceva.contest.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import co.uceva.contest.domain.model.InvitationStatus;
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
 * Entidad JPA de la tabla de invitaciones a equipos.
 * Es la representación persistente de {@link co.uceva.contest.domain.model.Invitation}.
 */
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "team_invitations")
public class InvitationEntity {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "team_id", nullable = false, updatable = false)
    private UUID teamId;

    @Column(name = "invited_user_id", nullable = false, updatable = false)
    private UUID invitedUserId;

    @Column(name = "invited_email", nullable = false, updatable = false)
    private String invitedEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InvitationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "responded_at")
    private Instant respondedAt;
}
