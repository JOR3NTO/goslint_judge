package co.uceva.contest.infrastructure.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Configuración del cliente HTTP hacia {@code problem-service}. */
@Configuration
public class ProblemServiceClientConfig {

    /**
     * Cliente con la URL base y los tiempos de espera hacia {@code problem-service}.
     *
     * @param builder          Builder autoconfigurado por Spring Boot.
     * @param baseUrl          URL base de {@code problem-service}.
     * @param connectTimeoutMs Tiempo máximo para conectar.
     * @param readTimeoutMs    Tiempo máximo de espera de la respuesta.
     * @return Cliente HTTP listo para consultar {@code problem-service}.
     */
    @Bean
    public RestClient problemServiceRestClient(RestClient.Builder builder,
            @Value("${app.problem-service.base-url}") String baseUrl,
            @Value("${app.problem-service.connect-timeout-ms:3000}") long connectTimeoutMs,
            @Value("${app.problem-service.read-timeout-ms:5000}") long readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }
}
