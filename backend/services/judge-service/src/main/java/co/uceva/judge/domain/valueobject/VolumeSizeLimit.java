package co.uceva.judge.domain.valueobject;

/**
 * Value Object que representa el tamaño máximo, en bytes, del volumen
 * escribible ({@code /work}) asignado a una solución dentro del sandbox.
 * Garantiza que el valor se encuentre dentro de los rangos válidos del dominio.
 */
public record VolumeSizeLimit(long bytes) {

    /** Tamaño mínimo permitido para el volumen escribible, en bytes (1 MB). */
    public static final long MIN_BYTES = 1_048_576L;
    /** Tamaño máximo permitido para el volumen escribible, en bytes (512 MB). */
    public static final long MAX_BYTES = 536_870_912L;
    /** Valor por defecto utilizado cuando no se especifica un límite (50 MB). */
    public static final long DEFAULT_BYTES = 52_428_800L;

    /**
     * Constructor compacto que valida los invariantes del dominio.
     *
     * @param bytes Tamaño máximo del volumen escribible, en bytes.
     */
    public VolumeSizeLimit {
        if (bytes < MIN_BYTES || bytes > MAX_BYTES) {
            throw new IllegalArgumentException(
                "El tamaño del volumen debe estar entre " + MIN_BYTES + " bytes ("
                + (MIN_BYTES / 1_048_576L) + " MB) y " + MAX_BYTES + " bytes ("
                + (MAX_BYTES / 1_048_576L) + " MB)"
            );
        }
    }

    /** @return El límite de tamaño de volumen por defecto. */
    public static VolumeSizeLimit ofDefault() {
        return new VolumeSizeLimit(DEFAULT_BYTES);
    }
}
