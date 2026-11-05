package co.uceva.submission.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Instantánea de solo lectura de un concurso, tal como la conoce {@code contest-service}.
 *
 * @param contestId Identificador del concurso.
 * @param startTime Instante de inicio (inclusivo).
 * @param endTime   Instante de finalización (exclusivo).
 */
public record ContestSnapshot(UUID contestId, Instant startTime, Instant endTime) {

    /**
     * Indica si el concurso está en curso en el instante dado.
     * <p>
     * El intervalo es semiabierto, {@code [inicio, fin)}: un envío recibido justo en
     * el instante de inicio es válido, y uno recibido justo en el de fin ya no. Así
     * dos lecturas del reloj en el mismo instante nunca dan respuestas distintas.
     * </p>
     *
     * @param now Instante a evaluar.
     * @return {@code true} si {@code now} está dentro del intervalo del concurso.
     */
    public boolean isActiveAt(Instant now) {
        return !now.isBefore(startTime) && now.isBefore(endTime);
    }
}
