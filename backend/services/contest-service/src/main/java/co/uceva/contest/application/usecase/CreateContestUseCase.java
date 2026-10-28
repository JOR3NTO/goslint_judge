package co.uceva.contest.application.usecase;

import java.time.Instant;
import java.util.UUID;

import co.uceva.contest.domain.model.Contest;

/**
 * Puerto de entrada para el caso de uso de creación de un concurso.
 */
public interface CreateContestUseCase {
    /**
     * Ejecuta la creación de un nuevo concurso.
     *
     * @param command Datos necesarios para crear el concurso.
     * @return El concurso creado.
     */
    Contest execute(CreateContestCommand command);

    /**
     * Comando inmutable que agrupa los datos de entrada para crear un concurso.
     *
     * @param createdBy   Identificador del usuario creador.
     * @param title       Título del concurso.
     * @param description Descripción del concurso.
     * @param password    Contraseña en texto plano para la inscripción de equipos.
     * @param startTime   Fecha y hora de inicio.
     * @param endTime     Fecha y hora de finalización.
     */
    record CreateContestCommand(
        UUID createdBy,
        String title,
        String description,
        String password,
        Instant startTime,
        Instant endTime
    ){}
}
