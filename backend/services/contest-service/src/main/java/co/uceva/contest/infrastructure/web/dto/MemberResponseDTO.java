package co.uceva.contest.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de respuesta con los datos de un integrante.
 *
 * @param id           Identificador de la membresía.
 * @param teamId       Identificador del equipo.
 * @param userId       Identificador del usuario.
 * @param registeredAt Fecha y hora en que se unió al equipo.
 */
public record MemberResponseDTO(UUID id, UUID teamId, UUID userId, Instant registeredAt) {}
