package co.uceva.judge.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Límites de los monitores del sandbox, tanto en la respuesta como en la
 * petición de modificación (que reemplaza los cinco valores).
 *
 * @param outputSizeBytes Tamaño máximo de la salida estándar (stdout), en bytes.
 * @param errorSizeBytes  Tamaño máximo de la salida de error (stderr), en bytes.
 * @param hardTimePercent Margen del watchdog sobre el límite de tiempo, como fracción (0.3 = 30%).
 * @param watchIntervalMs Intervalo con el que el watchdog revisa el tiempo de CPU, en milisegundos.
 * @param absoluteTimeMs  Tiempo máximo absoluto de una ejecución, en milisegundos.
 */
public record MonitorLimitsDTO(
        @NotNull Long outputSizeBytes,
        @NotNull Long errorSizeBytes,
        @NotNull Float hardTimePercent,
        @NotNull Long watchIntervalMs,
        @NotNull Long absoluteTimeMs
) {}
