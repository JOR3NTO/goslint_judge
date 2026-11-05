package co.uceva.submission.application.usecase.impl;

import co.uceva.submission.application.usecase.GetAllSubmissionsUseCase;
import co.uceva.submission.domain.model.Submission;
import co.uceva.submission.domain.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de Aplicación que implementa la consulta de todos los envíos.
 */
@Service
public class GetAllSubmissionsUseCaseImpl implements GetAllSubmissionsUseCase {

    private final SubmissionRepository submissionRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param submissionRepository Puerto de salida para consultar envíos.
     */
    public GetAllSubmissionsUseCaseImpl(SubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    /**
     * Recupera todos los envíos registrados en el sistema.
     *
     * @return Lista de todos los envíos.
     */
    @Override
    @Transactional
    public List<Submission> execute() {
        return submissionRepository.findAll();
    }
}
