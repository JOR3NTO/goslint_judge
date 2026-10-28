package co.uceva.contest.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

import co.uceva.contest.domain.model.ClarificationStatus;

/**
 * DTO de respuesta con los datos de una aclaración.
 *
 * @param id         Identificador de la aclaración.
 * @param problemId  Identificador del problema del concurso ({@code ContestProblem}).
 * @param teamId     Identificador del equipo que preguntó.
 * @param question   Pregunta del equipo.
 * @param answer     Respuesta de los organizadores; nula si está pendiente.
 * @param status     Estado de la aclaración.
 * @param createdAt  Fecha y hora en que se envió la pregunta.
 * @param answeredAt Fecha y hora en que se respondió; nula si está pendiente.
 */
public record ClarificationResponseDTO(
    UUID id,
    UUID problemId,
    UUID teamId,
    String question,
    String answer,
    ClarificationStatus status,
    Instant createdAt,
    Instant answeredAt
) {}
