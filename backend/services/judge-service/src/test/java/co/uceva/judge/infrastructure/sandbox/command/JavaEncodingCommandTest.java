package co.uceva.judge.infrastructure.sandbox.command;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import co.uceva.shared.domain.ProgrammingLanguage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El contenedor del juez no tiene locale, así que la JVM y javac asumen
 * US-ASCII salvo que se les diga lo contrario. Sin estos argumentos, una
 * solución Java con tildes no compila y una que lee o imprime una "ñ" recibe
 * WRONG_ANSWER.
 */
class JavaEncodingCommandTest {

    @Test
    void shouldCompileJavaSourcesAsUtf8() {
        List<String> command = CompilationCommandFactory.build(ProgrammingLanguage.JAVA, "/tmp/src/Solution.java",
                Path.of("/tmp/src"));

        assertThat(command).containsSubsequence("javac", "-encoding", "UTF-8");
        assertThat(command).endsWith("/tmp/src/Solution.java");
    }

    @Test
    void shouldRunJavaSolutionsWithUtf8StandardStreams() {
        List<String> command = RunCommandFactory.build(ProgrammingLanguage.JAVA, "/tmp/src/Solution.java");

        assertThat(command).containsExactly("java", "-Dfile.encoding=UTF-8", "-cp", "/solution", "Solution");
    }
}
