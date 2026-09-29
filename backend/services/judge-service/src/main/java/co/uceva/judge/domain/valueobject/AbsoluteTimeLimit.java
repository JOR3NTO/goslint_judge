package co.uceva.judge.domain.valueobject;

/**
 * Value Object que representa el tiempo máximo absoluto, en milisegundos, que
 * puede durar la ejecución de una solución dentro del sandbox, sin importar
 * el límite de tiempo configurado para el problema. Actúa como una cota de
 * seguridad final del watchdog.
 * Garantiza que el valor se encuentre dentro de los rangos válidos del dominio.
 */
public record AbsoluteTimeLimit(long milliseconds) {

    /** Tiempo máximo absoluto mínimo permitido, en milisegundos (1 segundo). */
    public static final long MIN_MS = 1_000L;
    /** Tiempo máximo absoluto máximo permitido, en milisegundos (60 segundos). */
    public static final long MAX_MS = 60_000L;
    /** Valor por defecto utilizado cuando no se especifica un límite (10 segundos). */
    public static final long DEFAULT_MS = 10_000L;

    /**
     * Constructor compacto que valida los invariantes del dominio.
     *
     * @param milliseconds Tiempo máximo absoluto de ejecución, en milisegundos.
     */
    public AbsoluteTimeLimit {
        if (milliseconds < MIN_MS || milliseconds > MAX_MS) {
            throw new IllegalArgumentException(
                "El tiempo máximo absoluto debe estar entre " + MIN_MS + " ms y " + MAX_MS + " ms"
            );
        }
    }

    /** @return El tiempo máximo absoluto por defecto. */
    public static AbsoluteTimeLimit ofDefault() {
        return new AbsoluteTimeLimit(DEFAULT_MS);
    }
}
