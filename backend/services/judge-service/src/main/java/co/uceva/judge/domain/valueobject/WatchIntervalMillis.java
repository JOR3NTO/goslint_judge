package co.uceva.judge.domain.valueobject;

/**
 * Value Object que representa el intervalo, en milisegundos, con el que el
 * watchdog vigila el tiempo de CPU consumido por el proceso en ejecución.
 * Garantiza que el valor se encuentre dentro de los rangos válidos del dominio.
 */
public record WatchIntervalMillis(long milliseconds) {

    /** Intervalo mínimo permitido, en milisegundos (evita una vigilancia demasiado costosa). */
    public static final long MIN_MS = 50L;
    /** Intervalo máximo permitido, en milisegundos (evita una vigilancia demasiado laxa). */
    public static final long MAX_MS = 5_000L;
    /** Valor por defecto utilizado cuando no se especifica un intervalo. */
    public static final long DEFAULT_MS = 500L;

    /**
     * Constructor compacto que valida los invariantes del dominio.
     *
     * @param milliseconds Intervalo de vigilancia, en milisegundos.
     */
    public WatchIntervalMillis {
        if (milliseconds < MIN_MS || milliseconds > MAX_MS) {
            throw new IllegalArgumentException(
                "El intervalo de vigilancia debe estar entre " + MIN_MS + " ms y " + MAX_MS + " ms"
            );
        }
    }

    /** @return El intervalo de vigilancia por defecto. */
    public static WatchIntervalMillis ofDefault() {
        return new WatchIntervalMillis(DEFAULT_MS);
    }
}
