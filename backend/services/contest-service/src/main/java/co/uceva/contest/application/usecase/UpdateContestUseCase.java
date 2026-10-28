package co.uceva.contest.application.usecase;

import java.time.Instant;
import java.util.UUID;

import co.uceva.contest.domain.model.Contest;
import co.uceva.contest.domain.model.ContestStatus;

/**
 * Puerto de entrada para el caso de uso de actualización de un concurso.
 */
public interface UpdateContestUseCase {
    /**
     * Ejecuta la actualización de un concurso existente.
     *
     * @param command Datos necesarios para actualizar el concurso.
     * @return El concurso actualizado.
     */
    Contest execute(UpdateContestCommand command);

    /**
     * Comando inmutable que agrupa los datos de entrada para actualizar un concurso.
     *
     * @param contestId   Identificador del concurso a actualizar.
     * @param title       Nuevo título.
     * @param description Nueva descripción.
     * @param password    Nueva contraseña en texto plano; si es nula o vacía se conserva la actual.
     * @param startTime   Nueva fecha de inicio.
     * @param endTime     Nueva fecha de finalización.
     * @param status      Nuevo estado del concurso.
     */
    record UpdateContestCommand(
        UUID contestId,
        String title,
        String description,
        String password,
        Instant startTime,
        Instant endTime,
        ContestStatus status
    ){}
}
