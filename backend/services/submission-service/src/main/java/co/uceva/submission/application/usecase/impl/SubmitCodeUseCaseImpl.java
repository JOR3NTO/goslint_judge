package co.uceva.submission.application.usecase.impl;

import co.uceva.submission.application.port.out.SubmissionEventPublisher;
import co.uceva.submission.application.usecase.SubmitCodeUseCase;
import co.uceva.submission.domain.exception.DuplicateSubmissionException;
import co.uceva.submission.domain.model.Submission;
import co.uceva.submission.domain.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de Aplicación que implementa el caso de uso de recepción de un nuevo envío.
 * <p>
 * Orquesta la validación de duplicados, la construcción de la entidad de dominio,
 * su persistencia y la publicación del evento de envío recibido.
 * </p>
 */
@Service
public class SubmitCodeUseCaseImpl implements SubmitCodeUseCase {

    private final SubmissionRepository submissionRepository;
    private final SubmissionEventPublisher submissionEventPublisher;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param submissionRepository     Puerto de salida para persistir envíos.
     * @param submissionEventPublisher Puerto de salida para publicar eventos de envío.
     */
    public SubmitCodeUseCaseImpl(SubmissionRepository submissionRepository,
            SubmissionEventPublisher submissionEventPublisher) {
        this.submissionRepository = submissionRepository;
        this.submissionEventPublisher = submissionEventPublisher;
    }

    /**
     * Ejecuta el flujo de recepción de un nuevo envío:
     * <ol>
     *   <li>Valida que no exista un envío duplicado del mismo equipo, problema y código fuente.</li>
     *   <li>Construye la entidad de dominio a partir del comando.</li>
     *   <li>Persiste el envío a través del repositorio.</li>
     *   <li>Publica el evento de envío recibido para que el motor de juzgamiento lo procese.</li>
     * </ol>
     *
     * @param command Datos de entrada para crear el envío.
     * @return El envío creado y persistido.
     * @throws DuplicateSubmissionException Si ya existe un envío idéntico.
     */
    @Override
    @Transactional
    public Submission execute(SubmitCodeCommand command) {
        // Validar que no exista un envío duplicado
        if (submissionRepository.existsByTeamIdAndProblemIdAndSourceCode(
                command.teamId(), command.problemId(), command.sourceCode())) {
            throw new DuplicateSubmissionException(command.teamId(), command.problemId());
        }

        // Construir la entidad de dominio usando el factory method
        Submission submission = Submission.create(
                command.teamId(),
                command.problemId(),
                command.language(),
                command.sourceCode()
        );

        // Persistir el envío
        Submission saved = submissionRepository.save(submission);

        // Publicar evento para que judge-service lo procese
        submissionEventPublisher.publishSubmissionReceived(saved);

        return saved;
    }
}
