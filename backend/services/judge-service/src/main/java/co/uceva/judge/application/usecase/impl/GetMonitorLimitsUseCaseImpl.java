package co.uceva.judge.application.usecase.impl;

import org.springframework.stereotype.Service;

import co.uceva.judge.application.usecase.GetMonitorLimitsUseCase;
import co.uceva.judge.domain.model.MonitorLimits;
import co.uceva.judge.domain.repository.MonitorLimitsRepository;

/** Servicio de Aplicación que implementa la consulta de los límites de los monitores. */
@Service
public class GetMonitorLimitsUseCaseImpl implements GetMonitorLimitsUseCase {

    private final MonitorLimitsRepository monitorLimitsRepository;

    /**
     * @param monitorLimitsRepository Puerto de salida de los límites de los monitores.
     */
    public GetMonitorLimitsUseCaseImpl(MonitorLimitsRepository monitorLimitsRepository) {
        this.monitorLimitsRepository = monitorLimitsRepository;
    }

    @Override
    public MonitorLimits execute() {
        return monitorLimitsRepository.find();
    }
}
