package co.uceva.submission.application.usecase.impl;

import co.uceva.shared.domain.SubmissionStatus;
import co.uceva.submission.application.event.SubmissionStatusChangedEvent;
import co.uceva.submission.application.usecase.MarkSubmissionSystemErrorUseCase;
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
 * Servicio de Aplicación que cierra un envío que el motor de evaluación no
 * consiguió procesar.
 * <p>
 * Sigue exactamente el mismo camino que el registro de un veredicto normal:
 * persistir el nuevo estado y señalar el cambio. Que el desenlace sea un fallo
 * no cambia lo que el estudiante necesita, que es dejar de esperar.
 * </p>
 */
@Service
public class MarkSubmissionSystemErrorUseCaseImpl implements MarkSubmissionSystemErrorUseCase {

    private static final Logger log = LoggerFactory.getLogger(MarkSubmissionSystemErrorUseCaseImpl.class);

    private final SubmissionRepository submissionRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param submissionRepository      Puerto de salida para consultar y persistir envíos.
     * @param applicationEventPublisher Publicador de eventos internos de la aplicación.
     */
    public MarkSubmissionSystemErrorUseCaseImpl(SubmissionRepository submissionRepository,
            ApplicationEventPublisher applicationEventPublisher) {
        this.submissionRepository = submissionRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * Marca el envío con estado de error del sistema y señala el cambio.
     * <p>
     * Un envío que ya tenía veredicto se deja intacto y no se notifica nada: el
     * resultado válido que el estudiante ya recibió pesa más que un mensaje
     * averiado que llega tarde a la cola de fallidos.
     * </p>
     *
     * @param submissionId Identificador del envío que no pudo evaluarse.
     * @param reason       Motivo por el que se descartó, solo para trazabilidad.
     * @return El envío ya actualizado y persistido.
     * @throws SubmissionNotFoundException Si el envío no existe.
     */
    @Override
    @Transactional
    public Submission execute(UUID submissionId, String reason) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException(submissionId));

        submission.markSystemError();
        if (submission.getStatus() != SubmissionStatus.SYSTEM_ERROR) {
            log.warn("El envío {} ya tenía veredicto; se ignora el aviso de fallo de evaluación: {}",
                    submissionId, reason);
            return submission;
        }

        Submission saved = submissionRepository.save(submission);

        log.error("Envío {} marcado con estado {} tras agotar los reintentos de evaluación: {}",
                saved.getId(), saved.getStatus(), reason);

        applicationEventPublisher.publishEvent(new SubmissionStatusChangedEvent(saved));

        return saved;
    }
}
