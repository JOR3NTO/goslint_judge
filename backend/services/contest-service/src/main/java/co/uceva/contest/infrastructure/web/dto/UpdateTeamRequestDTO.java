package co.uceva.contest.infrastructure.web.dto;

/**
 * DTO de solicitud para renombrar un equipo.
 *
 * @param teamName Nuevo nombre del equipo.
 */
public record UpdateTeamRequestDTO(String teamName) {}
