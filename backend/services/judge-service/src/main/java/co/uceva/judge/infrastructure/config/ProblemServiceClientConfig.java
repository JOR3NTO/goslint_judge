package co.uceva.judge.infrastructure.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import co.uceva.judge.infrastructure.security.ServiceTokenProvider;

/** Configuración del cliente HTTP hacia {@code problem-service}. */
@Configuration
public class ProblemServiceClientConfig {

    /**
     * Cliente con la URL base, los tiempos de espera y un interceptor que firma
     * cada petición con un JWT {@code SERVICE} vigente, que es lo que exigen los
     * endpoints de casos de prueba.
     *
     * @param builder          Builder autoconfigurado por Spring Boot.
     * @param tokenProvider    Emisor del JWT de servicio.
     * @param baseUrl          URL base de {@code problem-service}.
     * @param connectTimeoutMs Tiempo máximo para conectar.
     * @param readTimeoutMs    Tiempo máximo de espera de la respuesta.
     * @return Cliente HTTP listo para consultar {@code problem-service}.
     */
    @Bean
    public RestClient problemServiceRestClient(RestClient.Builder builder, ServiceTokenProvider tokenProvider,
            @Value("${app.problem-service.base-url}") String baseUrl,
            @Value("${app.problem-service.connect-timeout-ms:3000}") long connectTimeoutMs,
            @Value("${app.problem-service.read-timeout-ms:10000}") long readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        return builder.baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.token());
                    return execution.execute(request, body);
                })
                .build();
    }
}
