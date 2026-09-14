package co.uceva.problem.domain.valueobject;

/**
 * Value Object que representa el límite de tiempo permitido para un problema.
 * Garantiza que el valor se encuentre dentro de los rangos válidos del dominio.
 */
public record TimeLimit(int milliseconds) {

    /** Límite mínimo de tiempo en milisegundos (100 ms). */
    public static final int MIN_MS = 100;
    /** Límite máximo de tiempo en milisegundos (5000 ms). */
    public static final int MAX_MS = 5_000;

    /**
     * Constructor compacto que valida los invariantes del dominio.
     *
     * @param milliseconds Límite de tiempo en milisegundos.
     */
    public TimeLimit {
        if (milliseconds < MIN_MS || milliseconds > MAX_MS) {
            throw new IllegalArgumentException(
                "El límite de tiempo debe estar entre " + MIN_MS + " ms y " + MAX_MS + " ms"
            );
        }
    }

    /** @return El límite de tiempo convertido a segundos. */
    public double toSeconds() {
        return milliseconds / 1000.0;
    }
}
