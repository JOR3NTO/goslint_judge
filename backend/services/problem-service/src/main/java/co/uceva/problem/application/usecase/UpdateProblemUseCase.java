package co.uceva.problem.application.usecase;

import java.util.UUID;

import co.uceva.problem.domain.model.Problem;

/**
 * Puerto de entrada para el caso de uso de actualización de un problema.
 */
public interface UpdateProblemUseCase {
    /**
     * Ejecuta la actualización de un problema existente.
     *
     * @param command Datos necesarios para actualizar el problema.
     * @return El problema actualizado.
     */
    Problem execute(UpdateProblemCommand command);

    /**
     * Comando inmutable que agrupa los datos de entrada para actualizar un problema.
     *
     * @param problemId        Identificador del problema a actualizar.
     * @param title            Nuevo título.
     * @param statement        Nuevo enunciado.
     * @param timeLimitMs      Nuevo límite de tiempo en milisegundos.
     * @param memoryLimitKb    Nuevo límite de memoria en kilobytes.
     * @param difficultyRating Nueva dificultad.
     * @param inputFormat      Nuevo formato de entrada.
     * @param outputFormat     Nuevo formato de salida.
     */
    record UpdateProblemCommand(
        UUID problemId,
        String title,
        String statement,
        int timeLimitMs,
        int memoryLimitKb,
        int difficultyRating,
        String inputFormat,
        String outputFormat
    ){}
}
