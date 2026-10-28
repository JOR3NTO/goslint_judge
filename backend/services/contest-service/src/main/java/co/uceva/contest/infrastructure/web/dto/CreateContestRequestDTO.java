package co.uceva.contest.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de solicitud para crear un nuevo concurso.
 *
 * @param createdBy   Identificador del usuario creador.
 * @param title       Título del concurso.
 * @param description Descripción del concurso.
 * @param password    Contraseña en texto plano para la inscripción de equipos.
 * @param startTime   Fecha y hora de inicio.
 * @param endTime     Fecha y hora de finalización.
 */
public record CreateContestRequestDTO(
    UUID createdBy,
    String title,
    String description,
    String password,
    Instant startTime,
    Instant endTime
) {}
