package co.uceva.contest.application.usecase.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contest.application.usecase.UpdateContestProblemUseCase;
import co.uceva.contest.domain.exception.ContestProblemNotFoundException;
import co.uceva.contest.domain.exception.ExistingContestProblemException;
import co.uceva.contest.domain.model.ContestProblem;
import co.uceva.contest.domain.repository.ContestProblemRepository;
import co.uceva.contest.domain.valueobject.BalloonColor;

/**
 * Servicio de Aplicación que implementa el caso de uso de actualización de un problema de concurso.
 */
@Service
public class UpdateContestProblemUseCaseImpl implements UpdateContestProblemUseCase {

    private final ContestProblemRepository contestProblemRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param contestProblemRepository Puerto de salida para gestionar problemas de concurso.
     */
    public UpdateContestProblemUseCaseImpl(ContestProblemRepository contestProblemRepository) {
        this.contestProblemRepository = contestProblemRepository;
    }

    /**
     * Ejecuta la actualización de un problema de concurso:
     * 1. Busca la asignación existente.
     * 2. Actualiza la entidad de dominio, que valida y normaliza los valores.
     * 3. Si el alias cambia, verifica que no esté en uso dentro del concurso.
     * 4. Persiste los cambios.
     *
     * @param command Datos de entrada para actualizar el problema.
     * @return El problema de concurso actualizado y persistido.
     * @throws ContestProblemNotFoundException  Si la asignación no existe.
     * @throws ExistingContestProblemException  Si el nuevo alias ya está en uso en el concurso.
     */
    @Override
    @Transactional
    public ContestProblem execute(UpdateContestProblemCommand command) {
        ContestProblem contestProblem = contestProblemRepository.findById(command.contestProblemId())
                .orElseThrow(() -> new ContestProblemNotFoundException(command.contestProblemId()));

        String oldAlias = contestProblem.getAlias();
        int penalty = command.penalty() != null
                ? command.penalty() : contestProblem.getPenalty();
        contestProblem.update(command.alias(), command.orderIndex(), command.points(), penalty,
                BalloonColor.of(command.balloonColor()));

        if (!oldAlias.equals(contestProblem.getAlias())
                && contestProblemRepository.existsByContestIdAndAlias(
                        contestProblem.getContestId(), contestProblem.getAlias())) {
            throw new ExistingContestProblemException(
                    "El alias '" + contestProblem.getAlias() + "' ya está en uso en el concurso '"
                            + contestProblem.getContestId() + "'.");
        }

        return contestProblemRepository.save(contestProblem);
    }
}
