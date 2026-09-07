package co.uceva.submission.application.usecase.impl;

import co.uceva.submission.application.event.SubmissionStatusChangedEvent;
import co.uceva.submission.application.usecase.UpdateSubmissionVerdictUseCase;
import co.uceva.submission.domain.exception.SubmissionNotFoundException;
import co.uceva.submission.domain.model.Submission;
import co.uceva.submission.domain.repository.SubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de Aplicación que implementa el registro del veredicto emitido por el
 * motor de evaluación.
 * <p>
 * Orquesta dos pasos que deben ocurrir en ese orden y no en el contrario:
 * primero se persiste el resultado y solo después se señala el cambio para
 * notificarlo. La notificación no forma parte de la transacción; se entrega
 * cuando el commit ya ocurrió, de modo que la pantalla del estudiante nunca
 * muestre un veredicto que la base de datos acabó descartando.
 * </p>
 */
@Service
public class UpdateSubmissionVerdictUseCaseImpl implements UpdateSubmissionVerdictUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateSubmissionVerdictUseCaseImpl.class);

    private final SubmissionRepository submissionRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param submissionRepository      Puerto de salida para consultar y persistir envíos.
     * @param applicationEventPublisher Publicador de eventos internos de la aplicación.
     */
    public UpdateSubmissionVerdictUseCaseImpl(SubmissionRepository submissionRepository,
            ApplicationEventPublisher applicationEventPublisher) {
        this.submissionRepository = submissionRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * Ejecuta el cierre del ciclo de vida del envío:
     * <ol>
     *   <li>Recupera el envío evaluado.</li>
     *   <li>Registra veredicto, tiempo de ejecución y memoria usada.</li>
     *   <li>Señala el cambio de estado para que se notifique tras el commit.</li>
     * </ol>
     *
     * @param command Resultado emitido por el motor de evaluación.
     * @return El envío ya actualizado y persistido.
     * @throws SubmissionNotFoundException Si el envío no existe.
     */
    @Override
    @Transactional
    public Submission execute(UpdateSubmissionVerdictCommand command) {
        Submission submission = submissionRepository.findById(command.submissionId())
                .orElseThrow(() -> new SubmissionNotFoundException(command.submissionId()));

        submission.updateVerdict(command.verdict(), command.executionTimeMs(), command.memoryUsedKb());
        Submission saved = submissionRepository.save(submission);

        log.info("Envío {} evaluado: veredicto {}, {} ms, {} KB.",
                saved.getId(), saved.getVerdict(), saved.getExecutionTimeMs(), saved.getMemoryUsedKb());

        applicationEventPublisher.publishEvent(new SubmissionStatusChangedEvent(saved));

        return saved;
    }
}
