package co.uceva.judge.application.usecase;

import co.uceva.judge.domain.model.MonitorLimits;

/** Puerto de entrada para consultar los límites vigentes de los monitores del sandbox. */
public interface GetMonitorLimitsUseCase {

    /** @return Los límites vigentes. */
    MonitorLimits execute();
}
