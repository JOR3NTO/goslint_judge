package co.uceva.contests_platform.contest.infrastructure.web.dto;

import java.util.UUID;

/**
 * DTO de solicitud para invitar a un estudiante al equipo.
 *
 * @param userId Identificador del usuario invitado; su correo se toma del usuario registrado.
 */
public record InviteMemberRequestDTO(UUID userId) {}
