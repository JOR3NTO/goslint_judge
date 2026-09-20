package co.uceva.judge.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import co.uceva.judge.infrastructure.sandbox.Runner;

/** Configuración del sandbox de ejecución. */
@Configuration
public class SandboxConfig {

    /**
     * Runner con los límites del dominio en sus valores por defecto.
     *
     * @return El orquestador de ejecución dentro del sandbox.
     */
    @Bean
    public Runner runner() {
        return new Runner();
    }
}
