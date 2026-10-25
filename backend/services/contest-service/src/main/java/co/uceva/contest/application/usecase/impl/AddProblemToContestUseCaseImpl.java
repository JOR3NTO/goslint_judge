package co.uceva.contest.application.usecase.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contest.application.usecase.AddProblemToContestUseCase;
import co.uceva.contest.domain.exception.ContestNotFoundException;
import co.uceva.contest.domain.exception.ExistingContestProblemException;
import co.uceva.contest.domain.exception.ProblemCatalogUnavailableException;
import co.uceva.contest.domain.exception.ProblemNotFoundException;
import co.uceva.contest.domain.model.ContestProblem;
import co.uceva.contest.domain.repository.ContestProblemRepository;
import co.uceva.contest.domain.repository.ContestRepository;
import co.uceva.contest.domain.service.ProblemCatalog;
import co.uceva.contest.domain.valueobject.BalloonColor;

/**
 * Servicio de Aplicación que implementa el caso de uso de asignación de problemas a un concurso.
 */
@Service
public class AddProblemToContestUseCaseImpl implements AddProblemToContestUseCase {

    private final ContestProblemRepository contestProblemRepository;
    private final ContestRepository contestRepository;
    private final ProblemCatalog problemCatalog;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param contestProblemRepository Puerto de salida para persistir problemas de concurso.
     * @param contestRepository        Puerto de salida para consultar concursos.
     * @param problemCatalog           Puerto de salida para verificar que el problema exista.
     */
    public AddProblemToContestUseCaseImpl(ContestProblemRepository contestProblemRepository,
                                          ContestRepository contestRepository,
                                          ProblemCatalog problemCatalog) {
        this.contestProblemRepository = contestProblemRepository;
        this.contestRepository = contestRepository;
        this.problemCatalog = problemCatalog;
    }

    /**
     * Ejecuta la asignación de un problema a un concurso:
     * 1. Verifica que el concurso exista.
     * 2. Construye la entidad, que valida y normaliza alias, puntos y color.
     * 3. Verifica que el problema y el alias no estén ya en uso en el concurso.
     * 4. Verifica en el catálogo que el problema exista.
     * 5. Persiste la asignación.
     *
     * @param command Datos de entrada para asignar el problema.
     * @return El problema de concurso creado y persistido.
     * @throws ContestNotFoundException         Si el concurso no existe.
     * @throws ExistingContestProblemException  Si el problema o el alias ya están en el concurso.
     * @throws ProblemNotFoundException         Si el problema no existe en el catálogo.
     * @throws ProblemCatalogUnavailableException Si no se pudo consultar el catálogo.
     */
    @Override
    @Transactional
    public ContestProblem execute(AddProblemToContestCommand command) {
        if (contestRepository.findById(command.contestId()).isEmpty()) {
            throw new ContestNotFoundException(command.contestId());
        }

        ContestProblem contestProblem = ContestProblem.builder()
                .contestId(command.contestId())
                .problemId(command.problemId())
                .alias(command.alias())
                .orderIndex(command.orderIndex())
                .points(command.points())
                .balloonColor(BalloonColor.of(command.balloonColor()))
                .build();

        if (contestProblemRepository.existsByContestIdAndProblemId(command.contestId(), command.problemId())) {
            throw new ExistingContestProblemException(command.problemId(), command.contestId());
        }
        if (contestProblemRepository.existsByContestIdAndAlias(command.contestId(), contestProblem.getAlias())) {
            throw new ExistingContestProblemException(
                    "El alias '" + contestProblem.getAlias() + "' ya está en uso en el concurso '"
                            + command.contestId() + "'.");
        }

        if (!problemCatalog.exists(command.problemId())) {
            throw new ProblemNotFoundException(command.problemId());
        }

        return contestProblemRepository.save(contestProblem);
    }
}
