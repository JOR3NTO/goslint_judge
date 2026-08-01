package co.uceva.problem.domain.valueobject;

public record MemoryLimit(int kilobytes) {

    public static final int MIN_KB = 4_096;     // 4 MB
    public static final int MAX_KB = 262_144; //  512 MB

    public MemoryLimit {
        if (kilobytes < MIN_KB || kilobytes > MAX_KB) {
            throw new IllegalArgumentException(
                "El límite de memoria debe estar entre " + MIN_KB + " KB (" + MIN_KB / 1024 + " MB) y "
                 + MAX_KB + " KB (" + MAX_KB / 1024 + " MB)"
            );
        }
    }

    public double toMegabytes() {
        return kilobytes / 1024.0;
    }
}
