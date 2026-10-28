package co.uceva.contest.infrastructure.web.dto;

/**
 * DTO de solicitud para crear un equipo en un concurso. Quien lo crea es el usuario autenticado
 * y queda como capitán.
 *
 * @param teamName        Nombre del equipo.
 * @param contestPassword Contraseña de inscripción del concurso, en texto plano.
 */
public record CreateTeamRequestDTO(String teamName, String contestPassword) {}
