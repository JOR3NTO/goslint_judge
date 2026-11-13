package co.uceva.contests_platform.contest.application.usecase.impl;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contests_platform.contest.application.usecase.FinishContestUseCase;
import co.uceva.contests_platform.contest.domain.model.ContestStatus;
import co.uceva.contests_platform.contest.domain.repository.ContestRepository;

/**
 * Servicio de Aplicación que finaliza un concurso al llegar su hora de fin.
 * Transiciona cualquier estado distinto de {@code ENDED} a {@code ENDED} cuando
 * {@code ahora >= endTime} (cubre también concursos que nunca llegaron a {@code ACTIVE}).
 */
@Service
public class FinishContestUseCaseImpl implements FinishContestUseCase {

    private final ContestRepository contestRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param contestRepository Puerto de salida para consultar y persistir concursos.
     */
    public FinishContestUseCaseImpl(ContestRepository contestRepository) {
        this.contestRepository = contestRepository;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Es idempotente: si el concurso no existe, ya cambió de estado o su hora aún no llega,
     * no hace nada.</p>
     */
    @Override
    @Transactional
    public void execute(UUID contestId) {
        contestRepository.findById(contestId)
                .filter(c -> c.getStatus() != ContestStatus.ENDED && !Instant.now().isBefore(c.getEndTime()))
                .ifPresent(c -> {
                    c.setStatus(ContestStatus.ENDED);
                    contestRepository.save(c);
                });
    }
}
