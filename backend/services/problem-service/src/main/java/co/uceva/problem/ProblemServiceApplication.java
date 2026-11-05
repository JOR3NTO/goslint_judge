package co.uceva.problem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la aplicación Spring Boot para el servicio de problemas.
 */
@SpringBootApplication
public class ProblemServiceApplication {

    /**
     * Método principal que inicia el contexto de Spring Boot.
     *
     * @param args Argumentos de línea de comandos.
     */
    public static void main(String[] args) {
        SpringApplication.run(ProblemServiceApplication.class, args);
    }
}
