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
 * <p>
 * Los nombres del exchange, las colas y las routing keys se leen de
 * {@code app.messaging.submission.*}, las mismas propiedades que usa
 * {@code RabbitSubmissionEventPublisherAdapter} para publicar. Así, cambiar una
 * propiedad mueve a la vez lo que se declara y lo que se envía, en lugar de dejar
 * el publicador apuntando a un exchange que nadie declaró. Esas propiedades son
 * también el contrato con el consumidor ({@code judge-service}).
 * </p>
 * <p>
 * La topología cubre los dos sentidos del diálogo con el motor de evaluación: la
 * cola de evaluación por la que salen los envíos, y la cola de veredictos por la
 * que vuelven ya juzgados. Ambas tienen su propia cola de mensajes muertos, de
 * modo que un envío que no pueda completar su recorrido acabe en un sitio
 * conocido en lugar de desaparecer.
 * </p>
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "app.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RabbitConfig {

    /**
     * Exchange principal, duradero para sobrevivir a reinicios del broker.
     *
     * @param exchange Nombre del exchange por el que se enrutan los envíos.
     * @return Exchange principal declarado en el broker al arrancar.
     */
    @Bean
    public TopicExchange submissionExchange(
            @Value("${app.messaging.submission.exchange}") String exchange) {
        return new TopicExchange(exchange, true, false);
    }

    /**
     * Exchange al que RabbitMQ reenvía los mensajes rechazados o expirados.
     *
     * @param deadLetterExchange Nombre del exchange de mensajes muertos.
     * @return Exchange de mensajes muertos.
     */
    @Bean
    public DirectExchange submissionDeadLetterExchange(
            @Value("${app.messaging.submission.dead-letter-exchange}") String deadLetterExchange) {
        return new DirectExchange(deadLetterExchange, true, false);
    }

    /**
     * Cola duradera de la que consume {@code judge-service}. Los mensajes que el
     * consumidor rechace definitivamente se derivan al exchange de mensajes muertos.
     *
     * @param queue              Nombre de la cola de evaluación.
     * @param deadLetterExchange Exchange al que derivar los mensajes rechazados.
     * @param deadLetterQueue    Routing key con la que se derivan, igual al nombre de la DLQ.
     * @return Cola de evaluación con su política de mensajes muertos.
     */
    @Bean
    public Queue submissionEvaluateQueue(
            @Value("${app.messaging.submission.queue}") String queue,
            @Value("${app.messaging.submission.dead-letter-exchange}") String deadLetterExchange,
            @Value("${app.messaging.submission.dead-letter-queue}") String deadLetterQueue) {
        return QueueBuilder.durable(queue)
                .deadLetterExchange(deadLetterExchange)
                .deadLetterRoutingKey(deadLetterQueue)
                .build();
    }

    /**
     * Cola donde quedan retenidos los envíos que no pudieron procesarse.
     *
     * @param deadLetterQueue Nombre de la cola de mensajes muertos.
     * @return Cola de mensajes muertos.
     */
    @Bean
    public Queue submissionEvaluateDeadLetterQueue(
            @Value("${app.messaging.submission.dead-letter-queue}") String deadLetterQueue) {
        return QueueBuilder.durable(deadLetterQueue).build();
    }

    /**
     * Enlaza la cola de evaluación con el exchange principal.
     * <p>
     * La routing key es la misma que publica el adaptador, de modo que el mensaje
     * siempre encuentra la cola.
     * </p>
     *
     * @param submissionEvaluateQueue Cola de evaluación.
     * @param submissionExchange      Exchange principal.
     * @param routingKey              Routing key de los envíos pendientes de evaluar.
     * @return Binding entre el exchange principal y la cola de evaluación.
     */
    @Bean
    public Binding submissionEvaluateBinding(Queue submissionEvaluateQueue,
            TopicExchange submissionExchange,
            @Value("${app.messaging.submission.routing-key}") String routingKey) {
        return BindingBuilder.bind(submissionEvaluateQueue)
                .to(submissionExchange)
                .with(routingKey);
    }

    /**
     * Enlaza la cola de mensajes muertos con su exchange.
     *
     * @param submissionEvaluateDeadLetterQueue Cola de mensajes muertos.
     * @param submissionDeadLetterExchange      Exchange de mensajes muertos.
     * @param deadLetterQueue                   Routing key usada al derivar, igual al nombre de la DLQ.
     * @return Binding entre el exchange de mensajes muertos y su cola.
     */
    @Bean
    public Binding submissionEvaluateDeadLetterBinding(Queue submissionEvaluateDeadLetterQueue,
            DirectExchange submissionDeadLetterExchange,
            @Value("${app.messaging.submission.dead-letter-queue}") String deadLetterQueue) {
        return BindingBuilder.bind(submissionEvaluateDeadLetterQueue)
                .to(submissionDeadLetterExchange)
                .with(deadLetterQueue);
    }

    /**
     * Cola duradera por la que {@code judge-service} devuelve los veredictos.
     * <p>
     * Un veredicto que este servicio no consiga registrar tras los reintentos se
     * deriva a su cola de mensajes muertos, donde
     * {@code ExhaustedSubmissionDeadLetterListener} lo recoge para cerrar el envío
     * con un estado de error del sistema.
     * </p>
     *
     * @param judgedQueue           Nombre de la cola de veredictos.
     * @param deadLetterExchange    Exchange al que derivar los mensajes rechazados.
     * @param judgedDeadLetterQueue Routing key con la que se derivan, igual al nombre de la DLQ.
     * @return Cola de veredictos con su política de mensajes muertos.
     */
    @Bean
    public Queue submissionJudgedQueue(
            @Value("${app.messaging.submission.judged-queue}") String judgedQueue,
            @Value("${app.messaging.submission.dead-letter-exchange}") String deadLetterExchange,
            @Value("${app.messaging.submission.judged-dead-letter-queue}") String judgedDeadLetterQueue) {
        return QueueBuilder.durable(judgedQueue)
                .deadLetterExchange(deadLetterExchange)
                .deadLetterRoutingKey(judgedDeadLetterQueue)
                .build();
    }

    /**
     * Cola donde quedan retenidos los veredictos que no pudieron registrarse.
     *
     * @param judgedDeadLetterQueue Nombre de la cola de mensajes muertos de veredictos.
     * @return Cola de mensajes muertos de veredictos.
     */
    @Bean
    public Queue submissionJudgedDeadLetterQueue(
            @Value("${app.messaging.submission.judged-dead-letter-queue}") String judgedDeadLetterQueue) {
        return QueueBuilder.durable(judgedDeadLetterQueue).build();
    }

    /**
     * Enlaza la cola de veredictos con el exchange principal.
     * <p>
     * La routing key es la que {@code judge-service} debe usar al publicar el
     * resultado de la evaluación: es el contrato entre ambos servicios.
     * </p>
     *
     * @param submissionJudgedQueue Cola de veredictos.
     * @param submissionExchange    Exchange principal.
     * @param judgedRoutingKey      Routing key de los envíos ya evaluados.
     * @return Binding entre el exchange principal y la cola de veredictos.
     */
    @Bean
    public Binding submissionJudgedBinding(Queue submissionJudgedQueue,
            TopicExchange submissionExchange,
            @Value("${app.messaging.submission.judged-routing-key}") String judgedRoutingKey) {
        return BindingBuilder.bind(submissionJudgedQueue)
                .to(submissionExchange)
                .with(judgedRoutingKey);
    }

    /**
     * Enlaza la cola de veredictos fallidos con el exchange de mensajes muertos.
     *
     * @param submissionJudgedDeadLetterQueue Cola de mensajes muertos de veredictos.
     * @param submissionDeadLetterExchange    Exchange de mensajes muertos.
     * @param judgedDeadLetterQueue           Routing key usada al derivar, igual al nombre de la DLQ.
     * @return Binding entre el exchange de mensajes muertos y la cola de veredictos fallidos.
     */
    @Bean
    public Binding submissionJudgedDeadLetterBinding(Queue submissionJudgedDeadLetterQueue,
            DirectExchange submissionDeadLetterExchange,
            @Value("${app.messaging.submission.judged-dead-letter-queue}") String judgedDeadLetterQueue) {
        return BindingBuilder.bind(submissionJudgedDeadLetterQueue)
                .to(submissionDeadLetterExchange)
                .with(judgedDeadLetterQueue);
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
