package co.uceva.contests_platform.contest.application.usecase.impl;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contests_platform.contest.application.usecase.StartContestUseCase;
import co.uceva.contests_platform.contest.domain.model.ContestStatus;
import co.uceva.contests_platform.contest.domain.repository.ContestRepository;

/**
 * Servicio de Aplicación que activa un concurso al llegar su hora de inicio.
 * Solo transiciona {@code UPCOMING → ACTIVE} cuando {@code ahora >= startTime}.
 */
@Service
public class StartContestUseCaseImpl implements StartContestUseCase {

    private final ContestRepository contestRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param contestRepository Puerto de salida para consultar y persistir concursos.
     */
    public StartContestUseCaseImpl(ContestRepository contestRepository) {
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
                .filter(c -> c.getStatus() == ContestStatus.UPCOMING && !Instant.now().isBefore(c.getStartTime()))
                .ifPresent(c -> {
                    c.setStatus(ContestStatus.ACTIVE);
                    contestRepository.save(c);
                });
    }
}
