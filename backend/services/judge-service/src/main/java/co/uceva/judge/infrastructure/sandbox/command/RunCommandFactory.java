package co.uceva.judge.infrastructure.sandbox.command;

import java.nio.file.Path;
import java.util.List;

import co.uceva.shared.domain.ProgrammingLanguage;

public final class RunCommandFactory {

    private RunCommandFactory() {}

    /**
     * Construye los argumentos que se pasarán a {@code exec} dentro del sandbox
     * para ejecutar la solución ya compilada (o interpretada).
     *
     * @param language     Lenguaje de la solución.
     * @param solutionPath Ruta del archivo fuente en el host (usada para derivar el nombre de la clase Java).
     * @return Argumentos del comando a ejecutar dentro del sandbox, o {@code null} si el lenguaje no está soportado.
     */
    public static List<String> build(ProgrammingLanguage language, String solutionPath) {
        if (language == null) {
            return null;
        }
        switch (language) {
            case C:
            case CPP:
                return List.of("/solution/solution");

            case JAVA:
                String filename = Path.of(solutionPath).getFileName().toString();
                String className = filename.contains(".")
                        ? filename.substring(0, filename.lastIndexOf('.'))
                        : filename;
                return List.of("java", "-cp", "/solution", className);

            case PYTHON:
                return List.of("python3", "/solution/solution.py");

            default:
                return null;
        }
    }
}
