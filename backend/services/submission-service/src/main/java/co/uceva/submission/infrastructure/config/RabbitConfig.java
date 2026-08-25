package co.uceva.submission.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuración de la topología de RabbitMQ usada para entregar los envíos al
 * motor de evaluación.
 * <p>
 * Todas las declaraciones son duraderas para que ni los mensajes ni las colas se
 * pierdan si el broker se reinicia. Los mensajes que no puedan procesarse acaban
 * en una cola de mensajes muertos en lugar de descartarse en silencio.
 * </p>
 * <p>
 * Se activa con {@code app.messaging.enabled}, que permite ejecutar el servicio
 * sin broker (por ejemplo en pruebas o en desarrollo local).
 * </p>
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "app.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RabbitConfig {

    /** Exchange principal por el que se enrutan los eventos de envíos. */
    public static final String SUBMISSION_EXCHANGE = "submission.exchange";
    /** Cola que consume el motor de evaluación. */
    public static final String SUBMISSION_EVALUATE_QUEUE = "submission.evaluate";
    /** Routing key de los envíos pendientes de evaluar. */
    public static final String SUBMISSION_EVALUATE_ROUTING_KEY = "submission.evaluate";
    /** Exchange de mensajes muertos. */
    public static final String SUBMISSION_DLX = "submission.dlx";
    /** Cola de mensajes muertos asociada a la cola de evaluación. */
    public static final String SUBMISSION_EVALUATE_DLQ = "submission.evaluate.dlq";

    /** Exchange principal, duradero para sobrevivir a reinicios del broker. */
    @Bean
    public TopicExchange submissionExchange() {
        return new TopicExchange(SUBMISSION_EXCHANGE, true, false);
    }

    /** Exchange al que RabbitMQ reenvía los mensajes rechazados o expirados. */
    @Bean
    public DirectExchange submissionDeadLetterExchange() {
        return new DirectExchange(SUBMISSION_DLX, true, false);
    }

    /**
     * Cola duradera de la que consume {@code judge-service}. Los mensajes que el
     * consumidor rechace definitivamente se derivan al exchange de mensajes muertos.
     */
    @Bean
    public Queue submissionEvaluateQueue() {
        return QueueBuilder.durable(SUBMISSION_EVALUATE_QUEUE)
                .deadLetterExchange(SUBMISSION_DLX)
                .deadLetterRoutingKey(SUBMISSION_EVALUATE_DLQ)
                .build();
    }

    /** Cola donde quedan retenidos los envíos que no pudieron procesarse. */
    @Bean
    public Queue submissionEvaluateDeadLetterQueue() {
        return QueueBuilder.durable(SUBMISSION_EVALUATE_DLQ).build();
    }

    /** Enlaza la cola de evaluación con el exchange principal. */
    @Bean
    public Binding submissionEvaluateBinding() {
        return BindingBuilder.bind(submissionEvaluateQueue())
                .to(submissionExchange())
                .with(SUBMISSION_EVALUATE_ROUTING_KEY);
    }

    /** Enlaza la cola de mensajes muertos con su exchange. */
    @Bean
    public Binding submissionEvaluateDeadLetterBinding() {
        return BindingBuilder.bind(submissionEvaluateDeadLetterQueue())
                .to(submissionDeadLetterExchange())
                .with(SUBMISSION_EVALUATE_DLQ);
    }

    /**
     * Serializa los eventos como JSON.
     * <p>
     * Reutiliza el {@link ObjectMapper} que Spring Boot ya configura, necesario
     * porque {@code SubmissionReceivedEvent.submittedAt} es un {@code Instant} y
     * requiere el módulo de fechas de Java 8.
     * </p>
     *
     * @param objectMapper Mapper autoconfigurado por Spring Boot.
     * @return Convertidor de mensajes basado en JSON.
     */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * Plantilla de publicación configurada para exigir confirmación del broker.
     * <p>
     * {@code mandatory} hace que un mensaje que no pueda enrutarse a ninguna cola
     * sea devuelto en lugar de descartarse, lo que permite detectar una topología
     * mal declarada en vez de dar por encolado un envío que nadie recibiría.
     * </p>
     *
     * @param connectionFactory Factoría de conexiones al broker.
     * @param messageConverter  Convertidor de mensajes a usar.
     * @param mandatory         Si los mensajes no enrutables deben devolverse.
     * @return Plantilla lista para publicar eventos.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
            MessageConverter messageConverter,
            @Value("${spring.rabbitmq.template.mandatory:true}") boolean mandatory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        rabbitTemplate.setMandatory(mandatory);
        return rabbitTemplate;
    }
}
