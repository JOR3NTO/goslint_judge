package co.uceva.contests_platform.submission.application.usecase.impl;

import co.uceva.contests_platform.submission.application.usecase.GetSubmissionByIdUseCase;
import co.uceva.contests_platform.submission.domain.exception.SubmissionNotFoundException;
import co.uceva.contests_platform.submission.domain.model.Submission;
import co.uceva.contests_platform.submission.domain.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Servicio de Aplicación que implementa la consulta de un envío por identificador.
 */
@Service
public class GetSubmissionByIdUseCaseImpl implements GetSubmissionByIdUseCase {

    private final SubmissionRepository submissionRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param submissionRepository Puerto de salida para consultar envíos.
     */
    public GetSubmissionByIdUseCaseImpl(SubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    /**
     * Busca un envío por su identificador y lanza una excepción si no existe.
     *
     * @param submissionId Identificador del envío.
     * @return El envío encontrado.
     * @throws SubmissionNotFoundException Si el envío no existe.
     */
    @Override
    @Transactional
    public Submission execute(UUID submissionId) {
        return submissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException(submissionId));
    }
}
