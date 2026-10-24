package co.uceva.contest.infrastructure.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO de respuesta con los datos de un equipo y sus integrantes.
 *
 * @param id          Identificador del equipo.
 * @param contestId   Identificador del concurso.
 * @param teamName    Nombre del equipo.
 * @param score       Puntos acumulados.
 * @param penaltyTime Tiempo de penalización acumulado.
 * @param createdAt   Fecha y hora de creación.
 * @param members     Integrantes del equipo.
 */
public record TeamResponseDTO(
    UUID id,
    UUID contestId,
    String teamName,
    int score,
    long penaltyTime,
    Instant createdAt,
    List<MemberResponseDTO> members
) {}
