package co.uceva.judge.domain.model;

import co.uceva.judge.domain.valueobject.AbsoluteTimeLimit;
import co.uceva.judge.domain.valueobject.ErrorSizeLimit;
import co.uceva.judge.domain.valueobject.HardTimePercent;
import co.uceva.judge.domain.valueobject.OutputSizeLimit;
import co.uceva.judge.domain.valueobject.WatchIntervalMillis;

/**
 * Límites con los que los monitores del sandbox vigilan una ejecución: el
 * tamaño máximo de la salida estándar, el de la salida de error y los márgenes del watchdog de
 * tiempo. Son parametrizables en tiempo de ejecución por un administrador.
 *
 * @param outputSize        Tamaño máximo de la salida estándar (stdout).
 * @param errorSize         Tamaño máximo de la salida de error (stderr).
 * @param hardTimePercent   Margen sobre el límite de tiempo antes de terminar el proceso.
 * @param watchInterval     Intervalo con el que el watchdog revisa el tiempo de CPU.
 * @param absoluteTimeLimit Tiempo máximo absoluto de una ejecución.
 */
public record MonitorLimits(OutputSizeLimit outputSize, ErrorSizeLimit errorSize, HardTimePercent hardTimePercent,
        WatchIntervalMillis watchInterval, AbsoluteTimeLimit absoluteTimeLimit) {

    /**
     * Constructor compacto que valida los invariantes del dominio.
     */
    public MonitorLimits {
        if (outputSize == null || errorSize == null || hardTimePercent == null || watchInterval == null || absoluteTimeLimit == null) {
            throw new IllegalArgumentException("Todos los límites de los monitores son obligatorios.");
        }
    }

    /** @return Los límites en sus valores por defecto. */
    public static MonitorLimits ofDefault() {
        return new MonitorLimits(OutputSizeLimit.ofDefault(), ErrorSizeLimit.ofDefault(), HardTimePercent.ofDefault(),
                WatchIntervalMillis.ofDefault(), AbsoluteTimeLimit.ofDefault());
    }
}
