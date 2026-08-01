package co.uceva.problem.domain.valueobject;

public record TimeLimit(int milliseconds) {

    public static final int MIN_MS = 100;
    public static final int MAX_MS = 5_000;
    public TimeLimit {
        if (milliseconds < MIN_MS || milliseconds > MAX_MS) {
            throw new IllegalArgumentException(
                "El límite de tiempo debe estar entre " + MIN_MS + " ms y " + MAX_MS + " ms"
            );
        }
    }

    public double toSeconds() {
        return milliseconds / 1000.0;
    }
}
