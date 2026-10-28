package co.uceva.contest.domain.valueobject;

import java.time.Instant;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Value Object que representa el rango de fechas (período de vigencia) de un concurso.
 * Encapsula la fecha de inicio y la fecha de finalización, garantizando
 * las reglas e invariantes de negocio temporales.
 */
@Getter
@EqualsAndHashCode
@ToString
public class ContestPeriod {

    /** Fecha y hora exacta de inicio del concurso. */
    private final Instant startTime;
    /** Fecha y hora exacta de finalización del concurso. */
    private final Instant endTime;

    /**
     * Constructor principal que valida los invariantes del dominio.
     * Exige que la fecha de inicio sea igual o posterior a la actual y que
     * la fecha de finalización sea estrictamente posterior a la fecha de inicio.
     *
     * @param startTime Fecha y hora de inicio.
     * @param endTime   Fecha y hora de finalización.
     */
    public ContestPeriod(Instant startTime, Instant endTime) {
        this(startTime, endTime, true);
    }

    /**
     * Constructor interno con opción de omitir la validación de fecha futura.
     *
     * @param startTime      Fecha y hora de inicio.
     * @param endTime        Fecha y hora de finalización.
     * @param validateFuture Si es {@code true}, exige que {@code startTime} no sea pasada.
     */
    private ContestPeriod(Instant startTime, Instant endTime, boolean validateFuture) {
        if (startTime == null) {
            throw new IllegalArgumentException("La fecha de inicio es obligatoria.");
        }
        if (endTime == null) {
            throw new IllegalArgumentException("La fecha de finalización es obligatoria.");
        }
        if (validateFuture && startTime.isBefore(Instant.now())) {
            throw new IllegalArgumentException("La fecha de inicio debe ser igual o posterior a la fecha actual.");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("La fecha de finalización debe ser posterior a la fecha de inicio.");
        }
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * Factory method para crear un nuevo período validando que la fecha de inicio
     * sea igual o posterior a la actual.
     *
     * @param startTime Fecha y hora de inicio.
     * @param endTime   Fecha y hora de finalización.
     * @return Una nueva instancia de {@link ContestPeriod}.
     */
    public static ContestPeriod of(Instant startTime, Instant endTime) {
        return new ContestPeriod(startTime, endTime, true);
    }

    /**
     * Factory method para reconstruir un período existente (por ejemplo, al recuperar
     * un concurso de base de datos que ya inició o finalizó).
     *
     * @param startTime Fecha y hora de inicio.
     * @param endTime   Fecha y hora de finalización.
     * @return Instancia de {@link ContestPeriod}.
     */
    public static ContestPeriod restore(Instant startTime, Instant endTime) {
        return new ContestPeriod(startTime, endTime, false);
    }

    /** @return La fecha y hora de inicio. */
    public Instant startTime() {
        return startTime;
    }

    /** @return La fecha y hora de finalización. */
    public Instant endTime() {
        return endTime;
    }
}

