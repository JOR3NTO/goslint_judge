package co.uceva.contest.domain.valueobject;

import java.util.Locale;
import java.util.regex.Pattern;

import co.uceva.contest.domain.exception.InvalidContestProblemException;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Value Object inmutable que representa el color del globo entregado a un equipo
 * al resolver un problema del concurso (tradición de las maratones ICPC).
 * <p>
 * Se almacena como color hexadecimal {@code #RRGGBB} en mayúsculas para que el
 * frontend lo use directamente.
 * </p>
 */
@Getter
@EqualsAndHashCode
public class BalloonColor {

    private static final Pattern HEX_PATTERN = Pattern.compile("^#[0-9A-F]{6}$");

    /** Color normalizado en formato {@code #RRGGBB}. */
    private final String value;

    /**
     * Constructor principal que valida y normaliza el color.
     *
     * @param value Color en formato {@code #RRGGBB}; se aceptan minúsculas y espacios alrededor.
     * @throws InvalidContestProblemException si el color es nulo o no tiene el formato esperado.
     */
    public BalloonColor(String value) {
        if (value == null) {
            throw new InvalidContestProblemException("El color del globo es obligatorio.");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!HEX_PATTERN.matcher(normalized).matches()) {
            throw new InvalidContestProblemException(
                    "El color del globo debe tener el formato hexadecimal #RRGGBB.");
        }
        this.value = normalized;
    }

    /**
     * Factory method para crear un {@link BalloonColor}.
     *
     * @param value Color en formato {@code #RRGGBB}.
     * @return Una nueva instancia de {@link BalloonColor}.
     */
    public static BalloonColor of(String value) {
        return new BalloonColor(value);
    }

    /**
     * Retorna el color normalizado.
     *
     * @return Color en formato {@code #RRGGBB}.
     */
    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
