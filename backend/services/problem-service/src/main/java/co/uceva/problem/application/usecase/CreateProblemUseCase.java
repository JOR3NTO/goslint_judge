package co.uceva.problem.application.usecase;

import java.util.UUID;

import co.uceva.problem.domain.model.Problem;

/**
 * Puerto de entrada para el caso de uso de creación de un problema.
 * Define el contrato que la capa de infraestructura debe invocar.
 */
public interface CreateProblemUseCase {
    /**
     * Ejecuta la creación de un nuevo problema.
     *
     * @param command Datos necesarios para crear el problema.
     * @return El problema creado.
     */
    Problem execute(CreateProblemCommand command);

    /**
     * Comando inmutable que agrupa los datos de entrada para crear un problema.
     *
     * @param createdBy     Identificador del usuario creador.
     * @param title         Título del problema.
     * @param statement     Enunciado del problema.
     * @param timeLimitMs   Límite de tiempo en milisegundos.
     * @param memoryLimitKb Límite de memoria en kilobytes.
     * @param difficulty    Nivel de dificultad.
     * @param inputFormat   Formato de entrada esperado.
     * @param outputFormat  Formato de salida esperado.
     */
    record CreateProblemCommand(
        UUID createdBy,
        String title,
        String statement,
        int timeLimitMs,
        int memoryLimitKb,
        int difficulty,
        String inputFormat,
        String outputFormat
    ){}
}