package co.uceva.contest.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

import co.uceva.contest.domain.model.ContestStatus;

/**
 * DTO de respuesta con los datos de un concurso. No expone la contraseña ni su hash.
 *
 * @param id          Identificador del concurso.
 * @param createdBy   Identificador del usuario creador.
 * @param title       Título del concurso.
 * @param description Descripción del concurso.
 * @param startTime   Fecha y hora de inicio.
 * @param endTime     Fecha y hora de finalización.
 * @param status      Estado del concurso.
 * @param createdAt   Fecha y hora de creación.
 */
public record ContestResponseDTO(
    UUID id,
    UUID createdBy,
    String title,
    String description,
    Instant startTime,
    Instant endTime,
    ContestStatus status,
    Instant createdAt
) {}
