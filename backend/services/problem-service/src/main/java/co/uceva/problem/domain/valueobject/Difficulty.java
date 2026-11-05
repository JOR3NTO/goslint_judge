package co.uceva.problem.domain.valueobject;

/**
 * Value Object que representa el nivel de dificultad de un problema.
 * Encapsula las reglas de validación propias del dominio.
 */
public record Difficulty(int difficult) {
    /** Dificultad mínima permitida. */
    public static int MIN_DIFFICULT = 800;
    /** Dificultad máxima permitida. */
    public static int MAX_DIFFICULT = 3500;

    /**
     * Constructor compacto que valida los invariantes del dominio.
     * La dificultad debe estar dentro del rango permitido y ser múltiplo de 100.
     *
     * @param difficult Valor numérico de dificultad.
     */
    public Difficulty {
        if (difficult < MIN_DIFFICULT || difficult > MAX_DIFFICULT) {
            throw new IllegalArgumentException(
                "La dificultad estar entre " + MIN_DIFFICULT + " y " + MAX_DIFFICULT
            );
        }
        if (difficult % 100 != 0) {
            throw new IllegalArgumentException("La dificultad debe ser un múltiplo de 100 (ej: 800, 1200, 1500)");
        }
    }
}
