package co.uceva.contest.application.usecase;

import java.util.UUID;

import co.uceva.contest.domain.model.Clarification;

/**
 * Puerto de entrada para el caso de uso en que un equipo pregunta sobre un problema del concurso.
 */
public interface AskClarificationUseCase {
    /**
     * Ejecuta el envío de una pregunta en nombre del equipo del usuario.
     *
     * @param command Datos necesarios para enviar la pregunta.
     * @return La aclaración creada, pendiente de respuesta.
     */
    Clarification execute(AskClarificationCommand command);

    /**
     * Comando inmutable que agrupa los datos de entrada para enviar una pregunta.
     *
     * @param problemId Identificador del problema del concurso ({@code ContestProblem}) sobre el que se pregunta.
     * @param userId    Identificador del usuario que pregunta; debe pertenecer a un equipo de ese concurso.
     * @param question  Texto de la pregunta.
     */
    record AskClarificationCommand(
        UUID problemId,
        UUID userId,
        String question
    ){}
}
