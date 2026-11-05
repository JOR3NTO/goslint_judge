package co.uceva.submission.infrastructure.messaging;

import co.uceva.submission.application.usecase.EnqueueSubmissionUseCase;
import co.uceva.submission.domain.model.Submission;
import co.uceva.submission.domain.repository.SubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Reintenta periódicamente la entrega de los envíos que quedaron sin encolar.
 * <p>
 * Es la red de seguridad que hace que un envío nunca se pierda: si el motor de
 * evaluación no estaba disponible en su momento, el envío sigue persistido en
 * estado {@code PENDING} y este barrido vuelve a intentarlo hasta lograrlo, sin
 * que el estudiante tenga que hacer nada.
 * </p>
 * <p>
 * Como el estado vive en la propia tabla de envíos, el reintento también cubre el
 * caso de que el servicio se detenga entre el registro del envío y su entrega:
 * al arrancar de nuevo, el barrido recoge el trabajo pendiente.
 * </p>
 */
@Component
@ConditionalOnProperty(prefix = "app.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PendingSubmissionRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(PendingSubmissionRetryScheduler.class);

    private final SubmissionRepository submissionRepository;
    private final EnqueueSubmissionUseCase enqueueSubmissionUseCase;
    private final long gracePeriodMs;
    private final int batchSize;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param submissionRepository     Puerto de salida para consultar envíos pendientes.
     * @param enqueueSubmissionUseCase Caso de uso que entrega el envío al motor de evaluación.
     * @param gracePeriodMs            Antigüedad mínima que debe tener un envío para reintentarlo.
     * @param batchSize                Número máximo de envíos a reintentar por ciclo.
     */
    public PendingSubmissionRetryScheduler(SubmissionRepository submissionRepository,
            EnqueueSubmissionUseCase enqueueSubmissionUseCase,
            @Value("${app.submission.retry.grace-period-ms:15000}") long gracePeriodMs,
            @Value("${app.submission.retry.batch-size:50}") int batchSize) {
        this.submissionRepository = submissionRepository;
        this.enqueueSubmissionUseCase = enqueueSubmissionUseCase;
        this.gracePeriodMs = gracePeriodMs;
        this.batchSize = batchSize;
    }

    /**
     * Recorre los envíos rezagados y vuelve a intentar encolarlos.
     * <p>
     * El periodo de gracia deja fuera los envíos recién registrados, que aún
     * están siendo entregados por el flujo normal, para no duplicar el trabajo.
     * </p>
     */
    @Scheduled(fixedDelayString = "${app.submission.retry.interval-ms:30000}")
    public void reintentarEnviosPendientes() {
        Instant umbral = Instant.now().minusMillis(gracePeriodMs);
        List<Submission> pendientes = submissionRepository.findStalePending(umbral, batchSize);

        if (pendientes.isEmpty()) {
            return;
        }

        log.info("Reintentando el encolamiento de {} envío(s) pendiente(s).", pendientes.size());
        pendientes.forEach(enqueueSubmissionUseCase::execute);
    }
}
