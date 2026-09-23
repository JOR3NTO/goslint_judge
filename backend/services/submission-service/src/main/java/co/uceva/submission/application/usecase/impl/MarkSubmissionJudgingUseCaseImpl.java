package co.uceva.submission.application.usecase.impl;

import co.uceva.shared.domain.SubmissionStatus;
import co.uceva.submission.application.event.SubmissionStatusChangedEvent;
import co.uceva.submission.application.usecase.MarkSubmissionJudgingUseCase;
import co.uceva.submission.domain.exception.SubmissionNotFoundException;
import co.uceva.submission.domain.model.Submission;
import co.uceva.submission.domain.repository.SubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Servicio de Aplicación que refleja que {@code judge-service} tomó un envío y
 * empezó a evaluarlo.
 * <p>
 * Sigue el mismo camino que registrar un veredicto o un error del sistema:
 * persistir el nuevo estado y señalar el cambio para que se notifique.
 * </p>
 */
@Service
public class MarkSubmissionJudgingUseCaseImpl implements MarkSubmissionJudgingUseCase {

    private static final Logger log = LoggerFactory.getLogger(MarkSubmissionJudgingUseCaseImpl.class);

    private final SubmissionRepository submissionRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param submissionRepository      Puerto de salida para consultar y persistir envíos.
     * @param applicationEventPublisher Publicador de eventos internos de la aplicación.
     */
    public MarkSubmissionJudgingUseCaseImpl(SubmissionRepository submissionRepository,
            ApplicationEventPublisher applicationEventPublisher) {
        this.submissionRepository = submissionRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * Marca el envío como en proceso de evaluación y señala el cambio.
     * <p>
     * Un envío que ya tiene un desenlace ({@code JUDGED} o {@code SYSTEM_ERROR})
     * se deja intacto y no se notifica nada: un aviso de inicio que llega tarde
     * no puede hacer parecer «en curso» un envío que el estudiante ya vio cerrado.
     * </p>
     *
     * @param submissionId Identificador del envío que el juez empezó a evaluar.
     * @return El envío ya actualizado y persistido.
     * @throws SubmissionNotFoundException Si el envío no existe.
     */
    @Override
    @Transactional
    public Submission execute(UUID submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException(submissionId));

        submission.markJudging();
        if (submission.getStatus() != SubmissionStatus.JUDGING) {
            log.warn("El envío {} ya tenía un desenlace ({}); se ignora el aviso de inicio de evaluación.",
                    submissionId, submission.getStatus());
            return submission;
        }

        Submission saved = submissionRepository.save(submission);

        log.info("Envío {} en evaluación.", saved.getId());

        applicationEventPublisher.publishEvent(new SubmissionStatusChangedEvent(saved));

        return saved;
    }
}
