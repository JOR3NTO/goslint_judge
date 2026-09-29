package co.uceva.judge.infrastructure.web.mapper;

import co.uceva.judge.domain.model.MonitorLimits;
import co.uceva.judge.domain.valueobject.AbsoluteTimeLimit;
import co.uceva.judge.domain.valueobject.ErrorSizeLimit;
import co.uceva.judge.domain.valueobject.HardTimePercent;
import co.uceva.judge.domain.valueobject.OutputSizeLimit;
import co.uceva.judge.domain.valueobject.WatchIntervalMillis;
import co.uceva.judge.infrastructure.web.dto.MonitorLimitsDTO;

/** Conversión entre {@link MonitorLimits} y su DTO web. */
public final class MonitorLimitsWebMapper {

    private MonitorLimitsWebMapper() {}

    /**
     * @param limits Límites del dominio.
     * @return El DTO de respuesta.
     */
    public static MonitorLimitsDTO toDto(MonitorLimits limits) {
        return new MonitorLimitsDTO(limits.outputSize().bytes(), limits.errorSize().bytes(),
                limits.hardTimePercent().percentage(),
                limits.watchInterval().milliseconds(), limits.absoluteTimeLimit().milliseconds());
    }

    /**
     * @param dto Petición ya validada como no nula.
     * @return Los límites del dominio.
     * @throws IllegalArgumentException Si algún valor está fuera del rango que permite su value object.
     */
    public static MonitorLimits toDomain(MonitorLimitsDTO dto) {
        return new MonitorLimits(new OutputSizeLimit(dto.outputSizeBytes()),
                new ErrorSizeLimit(dto.errorSizeBytes()), new HardTimePercent(dto.hardTimePercent()),
                new WatchIntervalMillis(dto.watchIntervalMs()), new AbsoluteTimeLimit(dto.absoluteTimeMs()));
    }
}
