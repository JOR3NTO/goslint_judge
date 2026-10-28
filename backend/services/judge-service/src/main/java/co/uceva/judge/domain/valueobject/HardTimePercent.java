package co.uceva.judge.domain.valueobject;

/**
 * Value Object que representa el porcentaje de tiempo adicional, sobre el
 * límite de tiempo configurado, que el watchdog tolera antes de terminar de
 * forma forzada el proceso en ejecución.
 * Garantiza que el valor se encuentre dentro de los rangos válidos del dominio.
 */
public record HardTimePercent(float percentage) {

    /** Porcentaje mínimo de margen permitido (sin margen adicional). */
    public static final float MIN_PERCENTAGE = 0.0f;
    /** Porcentaje máximo de margen permitido (hasta el triple del tiempo límite). */
    public static final float MAX_PERCENTAGE = 2.0f;
    /** Valor por defecto utilizado cuando no se especifica un margen (30%). */
    public static final float DEFAULT_PERCENTAGE = 0.3f;

    /**
     * Constructor compacto que valida los invariantes del dominio.
     *
     * @param percentage Porcentaje de margen, expresado como fracción (0.3 = 30%).
     */
    public HardTimePercent {
        if (percentage < MIN_PERCENTAGE || percentage > MAX_PERCENTAGE) {
            throw new IllegalArgumentException(
                "El porcentaje de tiempo maximo debe estar entre " + MIN_PERCENTAGE + " y " + MAX_PERCENTAGE
            );
        }
    }

    /** @return El porcentaje de margen por defecto. */
    public static HardTimePercent ofDefault() {
        return new HardTimePercent(DEFAULT_PERCENTAGE);
    }
}
