package co.uceva.judge.application.usecase;

import co.uceva.judge.domain.model.MonitorLimits;

/** Puerto de entrada para modificar los límites de los monitores del sandbox. */
public interface UpdateMonitorLimitsUseCase {

    /**
     * Reemplaza los límites vigentes.
     *
     * @param limits Nuevos límites, ya validados por sus value objects.
     * @return Los límites que quedaron vigentes.
     */
    MonitorLimits execute(MonitorLimits limits);
}
