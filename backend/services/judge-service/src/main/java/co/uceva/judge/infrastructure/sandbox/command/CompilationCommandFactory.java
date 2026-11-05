package co.uceva.judge.infrastructure.sandbox.command;

import java.nio.file.Path;
import java.util.List;

import co.uceva.shared.domain.ProgrammingLanguage;

/**
 * Fábrica encargada de construir los comandos del sistema necesarios para
 * compilar el código fuente de una solución antes de su ejecución en el sandbox.
 * <p>
 * Los lenguajes compilados como C, C++ y Java generan comandos para invocar
 * sus respectivos compiladores (gcc, g++, javac). Para Python se genera el comando
 * de verificación sintáctica previa mediante {@code py_compile}.
 * </p>
 */
public final class CompilationCommandFactory {

    private CompilationCommandFactory() {}

    /**
     * Construye la lista de argumentos para ejecutar la compilación de la solución.
     *
     * @param language     Lenguaje de programación de la solución.
     * @param solutionPath Ruta absoluta del archivo fuente a compilar.
     * @param workDir      Directorio de trabajo donde se generarán los binarios o archivos compilados.
     * @return Lista de argumentos del comando de compilación, o {@code null} si el lenguaje no requiere compilación.
     */
    public static List<String> build(ProgrammingLanguage language, String solutionPath, Path workDir) {
        if (language == null) {
            return null;
        }

        List<String> command = null;
        switch (language) {
            case C:
                command = List.of(
                    "gcc",
                    "-std=c17",
                    solutionPath,
                    "-o",
                    workDir.resolve("solution").toString(),
                    "-lm"
                );
                break;

            case CPP:
                command = List.of(
                    "g++",
                    "-std=gnu++20",
                    solutionPath,
                    "-o",
                    workDir.resolve("solution").toString()
                );
                break;

            case JAVA:
                // -encoding es obligatorio: SolutionFileWriter escribe en UTF-8, pero en el
                // contenedor no hay locale y javac asume US-ASCII, con lo que una tilde en
                // un comentario termina en "unmappable character" y COMPILATION_ERROR.
                command = List.of(
                    "javac",
                    "-encoding",
                    "UTF-8",
                    "-d",
                    workDir.toString(),
                    solutionPath
                );
                break;

            case PYTHON:
                command = List.of(
                    "python3",
                    "-m",
                    "py_compile",
                    solutionPath
                );
                break;
        }
        return command;
    }
}
