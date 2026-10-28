package co.uceva.submission.application.usecase.impl;

import co.uceva.submission.application.exception.EventPublishingException;
import co.uceva.submission.application.port.out.SubmissionEventPublisher;
import co.uceva.submission.application.usecase.EnqueueSubmissionUseCase;
import co.uceva.submission.domain.model.Submission;
import co.uceva.submission.domain.repository.SubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de Aplicación que implementa el caso de uso de encolamiento de un
 * envío hacia el motor de evaluación.
 */
@Service
public class EnqueueSubmissionUseCaseImpl implements EnqueueSubmissionUseCase {

    private static final Logger log = LoggerFactory.getLogger(EnqueueSubmissionUseCaseImpl.class);

    private final SubmissionRepository submissionRepository;
    private final SubmissionEventPublisher submissionEventPublisher;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param submissionRepository     Puerto de salida para persistir envíos.
     * @param submissionEventPublisher Puerto de salida para publicar eventos de envío.
     */
    public EnqueueSubmissionUseCaseImpl(SubmissionRepository submissionRepository,
            SubmissionEventPublisher submissionEventPublisher) {
        this.submissionRepository = submissionRepository;
        this.submissionEventPublisher = submissionEventPublisher;
    }

    /**
     * Ejecuta el flujo de encolamiento:
     * <ol>
     *   <li>Publica el evento y espera la confirmación del motor de evaluación.</li>
     *   <li>Solo si la entrega fue confirmada, marca el envío como encolado y lo persiste.</li>
     * </ol>
     * Se ejecuta en una transacción propia porque se invoca una vez que la
     * transacción del registro del envío ya hizo commit.
     * <p>
     * Un fallo de mensajería se registra pero no se propaga: el envío queda
     * intacto en estado {@code PENDING} y el reintento automático volverá a
     * intentarlo, de modo que nunca se pierde por un problema temporal de
     * comunicación entre servicios.
     * </p>
     *
     * @param submission Envío a encolar.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(Submission submission) {
        try {
            submissionEventPublisher.publishSubmissionReceived(submission);
        } catch (EventPublishingException e) {
            log.warn("El envío {} no pudo encolarse; permanece en espera para reintento automático.",
                    submission.getId(), e);
            return;
        }

        submission.markQueued();
        submissionRepository.save(submission);
        log.info("Envío {} encolado para evaluación.", submission.getId());
    }
}
