package co.uceva.contest.infrastructure.web.dto;

import java.util.UUID;

/**
 * DTO de solicitud para invitar a un estudiante al equipo.
 *
 * @param userId Identificador del usuario invitado.
 * @param email  Correo al que se envía la invitación.
 */
public record InviteMemberRequestDTO(UUID userId, String email) {}
