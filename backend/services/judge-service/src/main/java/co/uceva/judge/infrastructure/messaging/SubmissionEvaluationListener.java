package co.uceva.judge.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import co.uceva.judge.application.usecase.EvaluateSubmissionUseCase;
import co.uceva.judge.domain.exception.TestCasesNotFoundException;
import co.uceva.shared.domain.event.SubmissionReceivedEvent;

/**
 * Consume los envíos que {@code submission-service} publica para evaluar.
 * <p>
 * Traduce el mensaje a una invocación del caso de uso y decide qué hacer con el
 * fallo: uno transitorio (el sandbox o {@code problem-service} no responden) se
 * propaga para que el contenedor reintente y, agotados los intentos, el mensaje
 * pase a la DLQ; uno permanente (el problema no tiene casos de prueba, el envío
 * es inválido) se rechaza de inmediato sin gastar reintentos.
 * </p>
 */
@Component
public class SubmissionEvaluationListener {

    private static final Logger log = LoggerFactory.getLogger(SubmissionEvaluationListener.class);

    private final EvaluateSubmissionUseCase evaluateSubmissionUseCase;

    /**
     * @param evaluateSubmissionUseCase Caso de uso que evalúa el envío.
     */
    public SubmissionEvaluationListener(EvaluateSubmissionUseCase evaluateSubmissionUseCase) {
        this.evaluateSubmissionUseCase = evaluateSubmissionUseCase;
    }

    /**
     * Evalúa el envío recibido.
     *
     * @param event Envío publicado por {@code submission-service}.
     * @throws AmqpRejectAndDontRequeueException Si el envío nunca podrá evaluarse.
     */
    @RabbitListener(queues = "${app.messaging.submission.queue}")
    public void onSubmissionReceived(SubmissionReceivedEvent event) {
        try {
            evaluateSubmissionUseCase.execute(event);
        } catch (TestCasesNotFoundException | IllegalArgumentException e) {
            log.error("Envío {} descartado: no puede evaluarse.", event.submissionId(), e);
            throw new AmqpRejectAndDontRequeueException(e);
        }
    }
}
