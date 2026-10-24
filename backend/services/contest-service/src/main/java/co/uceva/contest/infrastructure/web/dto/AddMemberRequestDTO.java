package co.uceva.contest.infrastructure.web.dto;

import java.util.UUID;

/**
 * DTO de solicitud para que un administrador u organizador agregue un integrante a un equipo.
 *
 * @param userId Identificador del usuario a agregar.
 */
public record AddMemberRequestDTO(UUID userId) {}
