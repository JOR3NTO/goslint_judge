package co.uceva.submission.application.usecase.impl;

import co.uceva.submission.application.event.SubmissionPersistedEvent;
import co.uceva.submission.application.port.out.ContestDataPort;
import co.uceva.submission.application.usecase.SubmitCodeUseCase;
import co.uceva.submission.domain.exception.DuplicateSubmissionException;
import co.uceva.submission.domain.model.Submission;
import co.uceva.submission.domain.repository.SubmissionRepository;
import co.uceva.submission.domain.service.SubmissionEligibilityPolicy;
import co.uceva.submission.domain.service.SubmissionEligibilityPolicy.SubmissionAttempt;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Servicio de Aplicación que implementa el caso de uso de recepción de un nuevo envío.
 * <p>
 * Orquesta la validación de elegibilidad (concursante, concurso activo y problema
 * del concurso), la validación de duplicados, la construcción de la entidad de dominio
 * y su persistencia, y señala que el envío está listo para ser encolado.
 * </p>
 */
@Service
public class SubmitCodeUseCaseImpl implements SubmitCodeUseCase {

    private final SubmissionRepository submissionRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ContestDataPort contestDataPort;
    private final SubmissionEligibilityPolicy eligibilityPolicy;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param submissionRepository      Puerto de salida para persistir envíos.
     * @param applicationEventPublisher Publicador de eventos internos de la aplicación.
     * @param contestDataPort           Puerto de lectura de equipos y concursos de {@code contest-service}.
     * @param clock                     Reloj con el que se decide si el concurso está activo.
     */
    public SubmitCodeUseCaseImpl(SubmissionRepository submissionRepository,
            ApplicationEventPublisher applicationEventPublisher, ContestDataPort contestDataPort, Clock clock) {
        this.submissionRepository = submissionRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.contestDataPort = contestDataPort;
        this.eligibilityPolicy = new SubmissionEligibilityPolicy(clock);
    }

    /**
     * Ejecuta el flujo de recepción de un nuevo envío:
     * <ol>
     *   <li>Valida la elegibilidad del envío con {@link SubmissionEligibilityPolicy}: debe hacerse
     *       primero, porque no tiene sentido comparar duplicados de un envío que no se va a aceptar.</li>
     *   <li>Valida que no exista un envío duplicado del mismo equipo, problema y código fuente.</li>
     *   <li>Construye la entidad de dominio a partir del comando.</li>
     *   <li>Persiste el envío a través del repositorio.</li>
     *   <li>Señala que el envío está listo para encolarse hacia el motor de juzgamiento.</li>
     * </ol>
     * El encolamiento real ocurre una vez que esta transacción ha hecho commit,
     * de modo que un fallo del sistema de mensajería nunca deshaga un envío ya
     * aceptado ni el juez reciba un envío que la base de datos descartó.
     *
     * @param command Datos de entrada para crear el envío.
     * @return El envío creado y persistido.
     * @throws co.uceva.submission.domain.exception.SubmissionNotAllowedException Si el envío no es elegible.
     * @throws co.uceva.submission.application.exception.ContestServiceUnavailableException Si no se pudo
     *         consultar {@code contest-service}.
     * @throws DuplicateSubmissionException Si ya existe un envío idéntico.
     */
    @Override
    @Transactional
    public Submission execute(SubmitCodeCommand command) {
        // Validar la elegibilidad antes que nada
        eligibilityPolicy.authorize(
                new SubmissionAttempt(command.submitterId(), command.teamId(), command.problemId()),
                contestDataPort);

        // Validar que no exista un envío duplicado
        if (submissionRepository.existsByTeamIdAndProblemIdAndSourceCodeAndLanguage(
                command.teamId(), command.problemId(), command.sourceCode(), command.language())) {
            throw new DuplicateSubmissionException(command.teamId(), command.problemId(), command.language());
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

        // Señalar que el envío está listo para encolarse; se entregará tras el commit
        applicationEventPublisher.publishEvent(new SubmissionPersistedEvent(saved));

        return saved;
    }
}
