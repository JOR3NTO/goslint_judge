package co.uceva.contests_platform.contest.infrastructure.web.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import co.uceva.contests_platform.contest.domain.model.ContestStatus;
import co.uceva.contests_platform.contest.domain.repository.ContestRepository;
import lombok.RequiredArgsConstructor;

/**
 * Reprograma al arrancar la aplicación los concursos que aún no han finalizado, ya que las
 * tareas del {@link org.springframework.scheduling.TaskScheduler} viven en memoria y se pierden
 * al reiniciar. Si el inicio o el fin de alguno ocurrió mientras estuvo apagada, la tarea corre
 * de inmediato y el estado queda al día.
 */
@Component
@RequiredArgsConstructor
public class ContestSchedulerInitializer {

    private final ContestRepository contestRepository;
    private final ContestSchedulerService schedulerService;

    /** Se ejecuta cuando la aplicación está lista para recibir tráfico. */
    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        contestRepository.findAll().stream()
                .filter(c -> c.getStatus() != ContestStatus.ENDED)
                .forEach(schedulerService::scheduleContest);
    }
}
