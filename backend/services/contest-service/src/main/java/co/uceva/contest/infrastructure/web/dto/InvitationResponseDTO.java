package co.uceva.contest.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

import co.uceva.contest.domain.model.InvitationStatus;

/**
 * DTO de respuesta con los datos de una invitación.
 */
public record InvitationResponseDTO(
    UUID id,
    UUID teamId,
    UUID invitedUserId,
    String invitedEmail,
    InvitationStatus status,
    Instant createdAt,
    Instant respondedAt
) {}
