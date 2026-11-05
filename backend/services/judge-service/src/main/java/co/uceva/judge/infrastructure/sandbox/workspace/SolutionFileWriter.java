package co.uceva.judge.infrastructure.sandbox.workspace;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import co.uceva.judge.domain.exception.CompilationException;
import co.uceva.shared.domain.ProgrammingLanguage;

/**
 * Utilidad encargada de persistir el código fuente de una solución en el
 * sistema de archivos dentro del directorio de trabajo del sandbox.
 * <p>
 * Determina la extensión de archivo correspondiente según el
 * {@link ProgrammingLanguage} proporcionado ({@code .c}, {@code .cpp},
 * {@code .java}, {@code .py}) y escribe el contenido asegurando codificación
 * UTF-8.
 * </p>
 */
public final class SolutionFileWriter {

    private SolutionFileWriter() {}

    /**
     * Escribe el código fuente de la solución en un archivo dentro del directorio especificado.
     *
     * @param language     Lenguaje de programación de la solución, utilizado para determinar la extensión del archivo.
     * @param content      Contenido en texto plano del código fuente.
     * @param workDir      Ruta del directorio de trabajo donde se guardará el archivo.
     * @param submissionId Identificador único del envío evaluado.
     * @return Ruta absoluta del archivo fuente generado como cadena de texto.
     * @throws IOException Si ocurre un error de entrada/salida al escribir el archivo en disco.
     */
    public static String execute(ProgrammingLanguage language,
                                 String content,
                                 Path workDir,
                                 UUID submissionId) throws IOException, CompilationException {

        String extension = "";
        switch (language) {
            case C:
                extension = ".c";
                break;
            case CPP:
                extension = ".cpp";
                break;
            case JAVA:
                extension = ".java";
                break;
            case PYTHON:
                extension = ".py";
                break;
        }
        
        Path source;
        if(language == ProgrammingLanguage.JAVA){
            source = workDir.resolve(getClassName(content) + extension);
            
        }else{
            source = workDir.resolve("solution" + extension);
        }
        Files.writeString(source, content, StandardCharsets.UTF_8);

        return source.toString();
    }

    public static String getClassName(String content) {
        Pattern pattern = Pattern.compile("\\bclass\\s+([A-Za-z0-9_]+)");
        Matcher finder = pattern.matcher(content);

        if (finder.find()) {
            return finder.group(1);
        }else{
            throw new CompilationException("No se pudo encontrar el nombre de la clase.");
        }
    }
}
