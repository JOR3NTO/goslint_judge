package co.uceva.submission.application.usecase.impl;

import co.uceva.submission.application.usecase.GetSubmissionMetricsUseCase;
import co.uceva.submission.domain.exception.SubmissionNotFoundException;
import co.uceva.submission.domain.model.Submission;
import co.uceva.submission.domain.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Servicio de Aplicación que implementa la consulta de métricas de un envío.
 */
@Service
public class GetSubmissionMetricsUseCaseImpl implements GetSubmissionMetricsUseCase {

    private final SubmissionRepository submissionRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param submissionRepository Puerto de salida para consultar envíos.
     */
    public GetSubmissionMetricsUseCaseImpl(SubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    /**
     * Recupera las métricas de evaluación de un envío específico.
     *
     * @param submissionId Identificador del envío.
     * @return Las métricas del envío (veredicto, tiempo, memoria, tamaño de código).
     * @throws SubmissionNotFoundException Si el envío no existe.
     */
    @Override
    @Transactional
    public SubmissionMetrics execute(UUID submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException(submissionId));

        return new SubmissionMetrics(
                submission.getId(),
                submission.getVerdict(),
                submission.getExecutionTimeMs(),
                submission.getMemoryUsedKb(),
                submission.getCodeSizeBytes()
        );
    }
}
