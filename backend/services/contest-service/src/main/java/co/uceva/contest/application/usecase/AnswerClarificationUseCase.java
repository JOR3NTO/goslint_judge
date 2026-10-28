package co.uceva.contest.application.usecase;

import java.util.UUID;

import co.uceva.contest.domain.model.Clarification;

/**
 * Puerto de entrada para el caso de uso de respuesta de una aclaración por parte de los organizadores.
 */
public interface AnswerClarificationUseCase {
    /**
     * Ejecuta la respuesta de una aclaración pendiente.
     *
     * @param command Datos necesarios para responder.
     * @return La aclaración respondida.
     */
    Clarification execute(AnswerClarificationCommand command);

    /**
     * Comando inmutable que agrupa los datos de entrada para responder una aclaración.
     *
     * @param clarificationId Identificador de la aclaración a responder.
     * @param answer          Texto de la respuesta.
     */
    record AnswerClarificationCommand(
        UUID clarificationId,
        String answer
    ){}
}
