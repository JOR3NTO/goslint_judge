package co.uceva.submission.application.usecase.impl;

import co.uceva.submission.application.usecase.GetSubmissionsByProblemUseCase;
import co.uceva.submission.domain.model.Submission;
import co.uceva.submission.domain.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Servicio de Aplicación que implementa la consulta de todos los envíos
 * asociados a un problema específico.
 */
@Service
public class GetSubmissionsByProblemUseCaseImpl implements GetSubmissionsByProblemUseCase {

    private final SubmissionRepository submissionRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param submissionRepository Puerto de salida para consultar envíos.
     */
    public GetSubmissionsByProblemUseCaseImpl(SubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    /**
     * Recupera todos los envíos de un problema.
     *
     * @param problemId Identificador del problema.
     * @return Lista de envíos asociados al problema.
     */
    @Override
    @Transactional
    public List<Submission> execute(UUID problemId) {
        return submissionRepository.findByProblemId(problemId);
    }
}
