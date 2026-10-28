package co.uceva.submission.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Expone el reloj del sistema como bean para poder sustituirlo en las pruebas y
 * fijar el instante con el que se decide si un concurso está activo.
 */
@Configuration
public class ClockConfig {

    /** @return Reloj del sistema en UTC. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
