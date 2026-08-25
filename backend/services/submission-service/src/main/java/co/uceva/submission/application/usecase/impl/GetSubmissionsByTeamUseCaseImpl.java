package co.uceva.submission.application.usecase.impl;

import co.uceva.submission.application.usecase.GetSubmissionsByTeamUseCase;
import co.uceva.submission.domain.model.Submission;
import co.uceva.submission.domain.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Servicio de Aplicación que implementa la consulta de todos los envíos
 * realizados por un equipo específico.
 */
@Service
public class GetSubmissionsByTeamUseCaseImpl implements GetSubmissionsByTeamUseCase {

    private final SubmissionRepository submissionRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param submissionRepository Puerto de salida para consultar envíos.
     */
    public GetSubmissionsByTeamUseCaseImpl(SubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    /**
     * Recupera todos los envíos de un equipo.
     *
     * @param teamId Identificador del equipo.
     * @return Lista de envíos realizados por el equipo.
     */
    @Override
    @Transactional
    public List<Submission> execute(UUID teamId) {
        return submissionRepository.findByTeamId(teamId);
    }
}
