package co.uceva.submission.application.usecase.impl;

import co.uceva.submission.application.usecase.DeleteSubmissionUseCase;
import co.uceva.submission.domain.exception.SubmissionNotFoundException;
import co.uceva.submission.domain.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Servicio de Aplicación que implementa el caso de uso de eliminación de un envío.
 */
@Service
public class DeleteSubmissionUseCaseImpl implements DeleteSubmissionUseCase {

    private final SubmissionRepository submissionRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param submissionRepository Puerto de salida para gestionar envíos.
     */
    public DeleteSubmissionUseCaseImpl(SubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    /**
     * Ejecuta la eliminación de un envío verificando previamente su existencia.
     *
     * @param submissionId Identificador del envío a eliminar.
     * @throws SubmissionNotFoundException Si el envío no existe.
     */
    @Override
    @Transactional
    public void execute(UUID submissionId) {
        if (submissionRepository.findById(submissionId).isEmpty()) {
            throw new SubmissionNotFoundException(submissionId);
        }
        submissionRepository.deleteById(submissionId);
    }
}
