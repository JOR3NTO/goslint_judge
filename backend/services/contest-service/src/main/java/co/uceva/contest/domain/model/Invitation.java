package co.uceva.contest.domain.model;

import java.time.Instant;
import java.util.UUID;

import co.uceva.contest.domain.exception.InvalidInvitationException;
import lombok.Builder;
import lombok.Getter;

/**
 * Entidad de Dominio que representa la invitación de un capitán a un estudiante
 * para unirse a su equipo.
 */
@Getter
public class Invitation {

    private final UUID id;
    private final UUID teamId;
    private final UUID invitedUserId;
    private final String invitedEmail;
    private InvitationStatus status;
    private final Instant createdAt;
    private Instant respondedAt;

    @Builder
    private Invitation(UUID id, UUID teamId, UUID invitedUserId, String invitedEmail,
                       InvitationStatus status, Instant createdAt, Instant respondedAt) {
        if (teamId == null || invitedUserId == null) {
            throw new IllegalArgumentException("El equipo y el usuario invitado son obligatorios.");
        }
        if (invitedEmail == null || !invitedEmail.contains("@")) {
            throw new IllegalArgumentException("El correo del invitado no es válido.");
        }
        this.id = id != null ? id : UUID.randomUUID();
        this.teamId = teamId;
        this.invitedUserId = invitedUserId;
        this.invitedEmail = invitedEmail.trim();
        this.status = status != null ? status : InvitationStatus.PENDING;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.respondedAt = respondedAt;
    }

    public static Invitation create(UUID teamId, UUID invitedUserId, String invitedEmail) {
        return Invitation.builder().teamId(teamId).invitedUserId(invitedUserId).invitedEmail(invitedEmail).build();
    }

    public void accept() {
        respond(InvitationStatus.ACCEPTED);
    }

    public void reject() {
        respond(InvitationStatus.REJECTED);
    }

    public void expire() {
        respond(InvitationStatus.EXPIRED);
    }

    public boolean isPending() {
        return status == InvitationStatus.PENDING;
    }

    private void respond(InvitationStatus newStatus) {
        if (!isPending()) {
            throw new InvalidInvitationException("La invitación ya fue respondida o ya no es válida.");
        }
        this.status = newStatus;
        this.respondedAt = Instant.now();
    }
}
