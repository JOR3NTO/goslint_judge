package co.uceva.contest.infrastructure.web.dto;

/**
 * DTO de solicitud para enviar una pregunta sobre un problema del concurso.
 * Quien pregunta es el usuario autenticado, en nombre de su equipo.
 *
 * @param question Texto de la pregunta.
 */
public record AskClarificationRequestDTO(String question) {}
