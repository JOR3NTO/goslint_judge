package co.uceva.judge.domain.valueobject;

/**
 * Value Object que representa el tamaño máximo, en bytes, permitido para la
 * salida de error (stderr) producida por una solución dentro del sandbox.
 * Se limita aparte de la salida estándar porque un volcado de errores
 * excesivo indica un fallo de la solución, no una respuesta larga legítima.
 * Garantiza que el valor se encuentre dentro de los rangos válidos del dominio.
 */
public record ErrorSizeLimit(long bytes) {

    /** Tamaño mínimo permitido para la salida de error, en bytes (1 KB). */
    public static final long MIN_BYTES = 1_024L;
    /** Tamaño máximo permitido para la salida de error, en bytes (100 MB). */
    public static final long MAX_BYTES = 104_857_600L;
    /** Valor por defecto utilizado cuando no se especifica un límite (10 MB). */
    public static final long DEFAULT_BYTES = 10_485_760L;

    /**
     * Constructor compacto que valida los invariantes del dominio.
     *
     * @param bytes Tamaño máximo permitido para la salida de error, en bytes.
     */
    public ErrorSizeLimit {
        if (bytes < MIN_BYTES || bytes > MAX_BYTES) {
            throw new IllegalArgumentException(
                "El tamaño de la salida de error debe estar entre " + MIN_BYTES + " bytes ("
                + (MIN_BYTES / 1_024L) + " KB) y " + MAX_BYTES + " bytes ("
                + (MAX_BYTES / 1_048_576L) + " MB)"
            );
        }
    }

    /** @return El límite de tamaño de salida de error por defecto. */
    public static ErrorSizeLimit ofDefault() {
        return new ErrorSizeLimit(DEFAULT_BYTES);
    }
}
