package co.uceva.contests_platform.contest.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

import co.uceva.contests_platform.contest.domain.model.ContestStatus;
import co.uceva.contests_platform.contest.domain.model.ScoreboardStatus;

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
 * @param freezeMinutes    Minutos finales con la tabla pública congelada; 0 si no se congela.
 * @param scoreboardStatus Estado de la tabla pública: en vivo, congelada o liberada.
 */
public record ContestResponseDTO(
    UUID id,
    UUID createdBy,
    String title,
    String description,
    Instant startTime,
    Instant endTime,
    ContestStatus status,
    Instant createdAt,
    int freezeMinutes,
    ScoreboardStatus scoreboardStatus
) {}
