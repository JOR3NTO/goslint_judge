package co.uceva.judge.application.usecase.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import co.uceva.judge.application.usecase.UpdateMonitorLimitsUseCase;
import co.uceva.judge.domain.model.MonitorLimits;
import co.uceva.judge.domain.repository.MonitorLimitsRepository;

/** Servicio de Aplicación que implementa la modificación de los límites de los monitores. */
@Service
public class UpdateMonitorLimitsUseCaseImpl implements UpdateMonitorLimitsUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateMonitorLimitsUseCaseImpl.class);

    private final MonitorLimitsRepository monitorLimitsRepository;

    /**
     * @param monitorLimitsRepository Puerto de salida de los límites de los monitores.
     */
    public UpdateMonitorLimitsUseCaseImpl(MonitorLimitsRepository monitorLimitsRepository) {
        this.monitorLimitsRepository = monitorLimitsRepository;
    }

    @Override
    public MonitorLimits execute(MonitorLimits limits) {
        monitorLimitsRepository.save(limits);
        log.info("Límites de los monitores del sandbox actualizados: {}", limits);
        return limits;
    }
}
