package co.uceva.judge.infrastructure.sandbox;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import co.uceva.judge.infrastructure.sandbox.command.CompilationCommandFactory;
import co.uceva.shared.domain.ProgrammingLanguage;

/**
 * Componente encargado de la compilación de las soluciones antes de su
 * ejecución dentro del sandbox. Pendiente de implementación.
 */
public class Compiler {

    public static int compile(ProgrammingLanguage language, String solutionPath, Path workDir) {
        List<String> command = CompilationCommandFactory.build(language, solutionPath, workDir);
        if (command == null) {
            return 0;
        }
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        try {
            return pb.start().waitFor();
        } catch (IOException | InterruptedException ex) {
            System.getLogger(Compiler.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            Thread.currentThread().interrupt();
            return -1;
        }
    }
}
