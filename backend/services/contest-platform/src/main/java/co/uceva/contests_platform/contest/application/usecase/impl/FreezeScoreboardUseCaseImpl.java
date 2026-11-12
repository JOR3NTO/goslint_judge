package co.uceva.contests_platform.contest.application.usecase.impl;

import java.time.Instant;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contests_platform.contest.application.event.ScoreboardFrozenEvent;
import co.uceva.contests_platform.contest.application.usecase.FreezeScoreboardUseCase;
import co.uceva.contests_platform.contest.domain.model.ScoreboardStatus;
import co.uceva.contests_platform.contest.domain.repository.ContestRepository;

/**
 * Servicio de Aplicación que congela la tabla pública de un concurso al llegar su hora de congelamiento.
 * La invoca la tarea programada de {@code ContestSchedulerService}, igual que el inicio y el fin.
 */
@Service
public class FreezeScoreboardUseCaseImpl implements FreezeScoreboardUseCase {

    private final ContestRepository contestRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param contestRepository         Puerto de salida para consultar y persistir concursos.
     * @param applicationEventPublisher Publicador de eventos internos de la aplicación.
     */
    public FreezeScoreboardUseCaseImpl(ContestRepository contestRepository,
            ApplicationEventPublisher applicationEventPublisher) {
        this.contestRepository = contestRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Es idempotente: solo congela una tabla en vivo cuya hora de congelamiento ya llegó, así que
     * una tarea repetida u obsoleta, o una tabla ya liberada por el organizador, no cambia nada.</p>
     */
    @Override
    @Transactional
    public void execute(UUID contestId) {
        contestRepository.findById(contestId)
                .filter(c -> c.getScoreboardStatus() == ScoreboardStatus.LIVE
                        && c.getFreezeTime() != null && !Instant.now().isBefore(c.getFreezeTime()))
                .ifPresent(c -> {
                    c.setScoreboardStatus(ScoreboardStatus.FROZEN);
                    applicationEventPublisher.publishEvent(new ScoreboardFrozenEvent(contestRepository.save(c)));
                });
    }
}
