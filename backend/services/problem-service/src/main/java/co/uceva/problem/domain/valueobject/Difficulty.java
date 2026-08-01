package co.uceva.problem.domain.valueobject;

public record Difficulty(int difficult) {
    public static int MIN_DIFFICULT = 800;
    public static int MAX_DIFFICULT = 3500;

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
