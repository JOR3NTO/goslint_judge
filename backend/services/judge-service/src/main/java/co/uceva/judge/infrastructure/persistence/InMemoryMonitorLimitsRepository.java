package co.uceva.judge.infrastructure.persistence;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import co.uceva.judge.domain.model.MonitorLimits;
import co.uceva.judge.domain.repository.MonitorLimitsRepository;
import co.uceva.judge.domain.valueobject.AbsoluteTimeLimit;
import co.uceva.judge.domain.valueobject.ErrorSizeLimit;
import co.uceva.judge.domain.valueobject.HardTimePercent;
import co.uceva.judge.domain.valueobject.OutputSizeLimit;
import co.uceva.judge.domain.valueobject.WatchIntervalMillis;

/**
 * Almacén en memoria de los límites de los monitores. Arrancan con los valores
 * de {@code app.sandbox.monitor.*} y los cambios hechos por un administrador
 * valen hasta el próximo reinicio: {@code judge-service} no tiene base de datos.
 * <p>
 * El campo es {@code volatile} porque lo escribe un hilo HTTP y lo leen los
 * hilos de evaluación.
 * </p>
 */
@Component
public class InMemoryMonitorLimitsRepository implements MonitorLimitsRepository {

    private volatile MonitorLimits limits;

    /**
     * @param outputSizeBytes Tamaño máximo de la salida estándar, en bytes.
     * @param errorSizeBytes  Tamaño máximo de la salida de error, en bytes.
     * @param hardTimePercent Margen del watchdog sobre el límite de tiempo.
     * @param watchIntervalMs Intervalo de vigilancia del watchdog, en milisegundos.
     * @param absoluteTimeMs  Tiempo máximo absoluto de una ejecución, en milisegundos.
     */
    public InMemoryMonitorLimitsRepository(
            @Value("${app.sandbox.monitor.output-size-bytes:" + OutputSizeLimit.DEFAULT_BYTES + "}") long outputSizeBytes,
            @Value("${app.sandbox.monitor.error-size-bytes:" + ErrorSizeLimit.DEFAULT_BYTES + "}") long errorSizeBytes,
            @Value("${app.sandbox.monitor.hard-time-percent:" + HardTimePercent.DEFAULT_PERCENTAGE + "}") float hardTimePercent,
            @Value("${app.sandbox.monitor.watch-interval-ms:" + WatchIntervalMillis.DEFAULT_MS + "}") long watchIntervalMs,
            @Value("${app.sandbox.monitor.absolute-time-ms:" + AbsoluteTimeLimit.DEFAULT_MS + "}") long absoluteTimeMs) {
        this.limits = new MonitorLimits(new OutputSizeLimit(outputSizeBytes), new ErrorSizeLimit(errorSizeBytes), new HardTimePercent(hardTimePercent),
                new WatchIntervalMillis(watchIntervalMs), new AbsoluteTimeLimit(absoluteTimeMs));
    }

    @Override
    public MonitorLimits find() {
        return limits;
    }

    @Override
    public void save(MonitorLimits limits) {
        this.limits = limits;
    }
}
