package co.uceva.contest.infrastructure.web.dto;

import java.util.UUID;

/**
 * DTO de solicitud para crear un equipo en un concurso.
 *
 * @param teamName        Nombre del equipo.
 * @param creatorUserId   Identificador del usuario que crea el equipo y queda como primer integrante.
 * @param contestPassword Contraseña de inscripción del concurso, en texto plano.
 */
public record CreateTeamRequestDTO(String teamName, UUID creatorUserId, String contestPassword) {}
