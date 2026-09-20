package co.uceva.judge.domain.valueobject;

/**
 * Value Object que representa el tamaño máximo, en bytes, permitido para la
 * salida estándar y de error producidas por una solución dentro del sandbox.
 * Garantiza que el valor se encuentre dentro de los rangos válidos del dominio.
 */
public record OutputSizeLimit(long bytes) {

    /** Tamaño mínimo permitido para la salida capturada, en bytes (1 KB). */
    public static final long MIN_BYTES = 1_024L;
    /** Tamaño máximo permitido para la salida capturada, en bytes (100 MB). */
    public static final long MAX_BYTES = 104_857_600L;
    /** Valor por defecto utilizado cuando no se especifica un límite (10 MB). */
    public static final long DEFAULT_BYTES = 10_485_760L;

    /**
     * Constructor compacto que valida los invariantes del dominio.
     *
     * @param bytes Tamaño máximo permitido para la salida capturada, en bytes.
     */
    public OutputSizeLimit {
        if (bytes < MIN_BYTES || bytes > MAX_BYTES) {
            throw new IllegalArgumentException(
                "El tamaño de salida debe estar entre " + MIN_BYTES + " bytes ("
                + (MIN_BYTES / 1_024L) + " KB) y " + MAX_BYTES + " bytes ("
                + (MAX_BYTES / 1_048_576L) + " MB)"
            );
        }
    }

    /** @return El límite de tamaño de salida por defecto. */
    public static OutputSizeLimit ofDefault() {
        return new OutputSizeLimit(DEFAULT_BYTES);
    }
}
