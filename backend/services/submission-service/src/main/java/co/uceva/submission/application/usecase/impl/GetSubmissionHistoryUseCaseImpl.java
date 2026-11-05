package co.uceva.submission.application.usecase.impl;

import co.uceva.submission.application.usecase.GetSubmissionHistoryUseCase;
import co.uceva.submission.domain.model.Submission;
import co.uceva.submission.domain.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de Aplicación que implementa la consulta del historial de envíos
 * de un equipo en un problema específico.
 */
@Service
public class GetSubmissionHistoryUseCaseImpl implements GetSubmissionHistoryUseCase {

    private final SubmissionRepository submissionRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param submissionRepository Puerto de salida para consultar envíos.
     */
    public GetSubmissionHistoryUseCaseImpl(SubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    /**
     * Recupera los envíos realizados por un equipo en un problema específico.
     *
     * @param command Datos de filtro con el problema y el equipo (ambos obligatorios).
     * @return Lista de envíos del equipo en el problema indicado.
     */
    @Override
    @Transactional
    public List<Submission> execute(GetSubmissionHistoryCommand command) {
        return submissionRepository.findByProblemIdAndTeamId(
                command.problemId(), command.teamId());
    }
}
