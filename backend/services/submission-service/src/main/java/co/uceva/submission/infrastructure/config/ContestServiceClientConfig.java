package co.uceva.submission.infrastructure.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Configuración del cliente HTTP hacia {@code contest-service}.
 * <p>
 * Los tiempos de conexión y lectura son cortos a propósito: la consulta ocurre en
 * la ruta de recepción de un envío, y es mejor fallar pronto con un 503 que dejar
 * al estudiante esperando. El bean tiene nombre propio para no colisionar con
 * cualquier otro {@link RestClient} que se añada al servicio.
 * </p>
 */
@Configuration
public class ContestServiceClientConfig {

    /**
     * @param builder          Constructor de {@link RestClient} de Spring Boot.
     * @param baseUrl          URL base de {@code contest-service}.
     * @param connectTimeoutMs Tiempo máximo de conexión en milisegundos.
     * @param readTimeoutMs    Tiempo máximo de lectura en milisegundos.
     * @return Cliente configurado contra {@code contest-service}.
     */
    @Bean
    @Qualifier("contestServiceRestClient")
    public RestClient contestServiceRestClient(RestClient.Builder builder,
            @Value("${app.contest-service.base-url}") String baseUrl,
            @Value("${app.contest-service.connect-timeout-ms:3000}") long connectTimeoutMs,
            @Value("${app.contest-service.read-timeout-ms:5000}") long readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }
}
