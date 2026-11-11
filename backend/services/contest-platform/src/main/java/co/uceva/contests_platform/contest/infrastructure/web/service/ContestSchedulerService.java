package co.uceva.contests_platform.contest.infrastructure.web.service;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import co.uceva.contests_platform.contest.application.usecase.FinishContestUseCase;
import co.uceva.contests_platform.contest.application.usecase.StartContestUseCase;
import co.uceva.contests_platform.contest.domain.model.Contest;
import lombok.RequiredArgsConstructor;

/**
 * Servicio que programa con {@link TaskScheduler} el cambio automático de estado de un concurso:
 * {@code UPCOMING → ACTIVE} a la hora de inicio y {@code ACTIVE → ENDED} a la hora de fin.
 *
 * <p>Notas de diseño:</p>
 * <ul>
 *   <li>Un instante que ya pasó se ejecuta de inmediato.</li>
 *   <li>Las tareas no se cancelan al reprogramar: si se editan las fechas, las tareas viejas
 *       siguen en cola pero no hacen nada, porque los casos de uso validan la hora real.</li>
 *   <li>Las tareas viven en memoria; al reiniciar las recrea
 *       {@link ContestSchedulerInitializer}.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ContestSchedulerService {

    private final TaskScheduler taskScheduler;
    private final StartContestUseCase startContest;
    private final FinishContestUseCase finishContest;

    /**
     * Programa el inicio y el fin del concurso según su período. Se invoca al crear o
     * actualizar un concurso y al arrancar la aplicación.
     *
     * @param contest Concurso a programar (con su período ya definido).
     */
    // shortcut: tareas en memoria de una sola instancia; con varias réplicas, usar ShedLock/Quartz.
    public void scheduleContest(Contest contest) {
        var id = contest.getId();
        taskScheduler.schedule(() -> startContest.execute(id), contest.getStartTime());
        taskScheduler.schedule(() -> finishContest.execute(id), contest.getEndTime());
    }
}
