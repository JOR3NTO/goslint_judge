package co.uceva.contest.application.usecase.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contest.application.usecase.AnswerClarificationUseCase;
import co.uceva.contest.domain.exception.ClarificationNotFoundException;
import co.uceva.contest.domain.exception.InvalidClarificationException;
import co.uceva.contest.domain.model.Clarification;
import co.uceva.contest.domain.repository.ClarificationRepository;

/**
 * Servicio de Aplicación que implementa el caso de uso de respuesta de aclaraciones.
 */
@Service
public class AnswerClarificationUseCaseImpl implements AnswerClarificationUseCase {

    private final ClarificationRepository clarificationRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param clarificationRepository Puerto de salida para gestionar aclaraciones.
     */
    public AnswerClarificationUseCaseImpl(ClarificationRepository clarificationRepository) {
        this.clarificationRepository = clarificationRepository;
    }

    /**
     * Ejecuta la respuesta de una aclaración: la busca, la responde en el dominio y persiste el cambio.
     *
     * @param command Datos de entrada para responder.
     * @return La aclaración respondida y persistida.
     * @throws ClarificationNotFoundException Si la aclaración no existe.
     * @throws InvalidClarificationException  Si ya fue respondida o la respuesta es inválida.
     */
    @Override
    @Transactional
    public Clarification execute(AnswerClarificationCommand command) {
        Clarification clarification = clarificationRepository.findById(command.clarificationId())
                .orElseThrow(() -> new ClarificationNotFoundException(command.clarificationId()));

        clarification.answer(command.answer());
        return clarificationRepository.save(clarification);
    }
}
