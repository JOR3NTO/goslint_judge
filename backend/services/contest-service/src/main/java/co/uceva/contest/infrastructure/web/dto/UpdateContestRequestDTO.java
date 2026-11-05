package co.uceva.contest.infrastructure.web.dto;

import java.time.Instant;

import co.uceva.contest.domain.model.ContestStatus;

/**
 * DTO de solicitud para actualizar un concurso existente.
 *
 * @param title       Nuevo título.
 * @param description Nueva descripción.
 * @param password    Nueva contraseña en texto plano; si se omite se conserva la actual.
 * @param startTime   Nueva fecha de inicio.
 * @param endTime     Nueva fecha de finalización.
 * @param status      Nuevo estado del concurso.
 */
public record UpdateContestRequestDTO(
    String title,
    String description,
    String password,
    Instant startTime,
    Instant endTime,
    ContestStatus status
) {}
