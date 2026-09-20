package co.uceva.judge.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import co.uceva.judge.domain.repository.MonitorLimitsRepository;
import co.uceva.judge.domain.valueobject.PidsLimit;
import co.uceva.judge.domain.valueobject.VolumeSizeLimit;
import co.uceva.judge.infrastructure.sandbox.Runner;

/** Configuración del sandbox de ejecución. */
@Configuration
public class SandboxConfig {

    /**
     * Runner con los límites de procesos y volumen por defecto y los límites de
     * los monitores leídos del repositorio en cada ejecución.
     *
     * @param monitorLimitsRepository Fuente de los límites de los monitores.
     * @return El orquestador de ejecución dentro del sandbox.
     */
    @Bean
    public Runner runner(MonitorLimitsRepository monitorLimitsRepository) {
        return new Runner(PidsLimit.ofDefault(), VolumeSizeLimit.ofDefault(), monitorLimitsRepository);
    }
}
