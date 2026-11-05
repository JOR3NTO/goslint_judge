package co.uceva.judge.infrastructure.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Configuración de RabbitMQ de {@code judge-service}.
 * <p>
 * Solo registra el convertidor JSON: Spring Boot lo aplica solo al
 * {@code RabbitTemplate} y a los listeners. La topología (exchanges, colas, DLQ)
 * la declara {@code submission-service}, dueño del contrato.
 * </p>
 */
@Configuration
public class RabbitConfig {

    /**
     * Serializa los eventos como JSON reutilizando el {@link ObjectMapper} de
     * Spring Boot, necesario porque los eventos llevan {@code Instant}.
     *
     * @param objectMapper Mapper autoconfigurado por Spring Boot.
     * @return Convertidor de mensajes basado en JSON.
     */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
