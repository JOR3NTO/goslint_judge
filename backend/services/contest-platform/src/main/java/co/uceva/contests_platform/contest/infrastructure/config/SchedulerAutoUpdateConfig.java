package co.uceva.contests_platform.contest.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Configuración del {@link TaskScheduler} que ejecuta los cambios automáticos de estado
 * de los concursos (inicio y fin). Usado por
 * {@link co.uceva.contests_platform.contest.infrastructure.web.service.ContestSchedulerService}.
 */
@Configuration
public class SchedulerAutoUpdateConfig {

    /**
     * Planificador con un pool pequeño: las tareas son breves (una consulta y una actualización).
     * {@code removeOnCancelPolicy} evita acumular tareas canceladas en la cola.
     *
     * @return El {@link TaskScheduler} de los concursos.
     */
    @Bean
    public TaskScheduler contestTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("contest-status-");
        scheduler.setRemoveOnCancelPolicy(true);
        return scheduler;
    }
}
