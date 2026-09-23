package co.uceva.judge.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Declara, solo para pruebas, la parte de la topología de RabbitMQ que
 * {@code judge-service} necesita para funcionar contra un broker limpio.
 * <p>
 * En producción esta topología la declara {@code submission-service}, dueño del
 * contrato, y {@code judge-service} se limita a consumir y publicar. Eso deja las
 * pruebas de integración sin nada declarado: el broker no tiene
 * {@code submission.evaluate}, y {@code SimpleMessageListenerContainer} trata una
 * cola ausente como error fatal, así que el contenedor de listeners no arranca.
 * </p>
 * <p>
 * Se declara únicamente lo que este servicio toca: la cola de la que consume y la
 * de veredictos en la que publica, cada una con su cola de mensajes muertos. Los
 * argumentos son deliberadamente idénticos a los de {@code submission-service}: una
 * cola ya existente con argumentos distintos hace que el broker responda
 * {@code PRECONDITION_FAILED} y cierre el canal, de modo que una copia "parecida"
 * fallaría justo en el escenario que la prueba quiere cubrir.
 * </p>
 * <p>
 * No se activa sola. Una prueba la incorpora con {@code @Import(RabbitTestTopology.class)}
 * y el perfil {@code test}, que es donde viven los nombres.
 * </p>
 */
@TestConfiguration
public class RabbitTestTopology {

    /**
     * Exchange principal por el que circulan los envíos y los veredictos.
     *
     * @param exchange Nombre del exchange principal.
     * @return Exchange principal, duradero.
     */
    @Bean
    public TopicExchange submissionExchange(
            @Value("${app.messaging.submission.exchange}") String exchange) {
        return new TopicExchange(exchange, true, false);
    }

    /**
     * Exchange al que el broker reenvía lo que se rechaza definitivamente.
     *
     * @param deadLetterExchange Nombre del exchange de mensajes muertos.
     * @return Exchange de mensajes muertos, duradero.
     */
    @Bean
    public DirectExchange submissionDeadLetterExchange(
            @Value("${app.messaging.submission.dead-letter-exchange}") String deadLetterExchange) {
        return new DirectExchange(deadLetterExchange, true, false);
    }

    /**
     * Cola de la que consume {@code SubmissionEvaluationListener}.
     * <p>
     * Los dos argumentos de mensajes muertos no son decorativos: sin ellos, la
     * política de reintentos configurada en {@code application.properties}
     * ({@code max-attempts=3}, {@code default-requeue-rejected=false}) descarta en
     * silencio el mensaje agotado en lugar de derivarlo, y una prueba de la DLQ no
     * tendría nada que observar.
     * </p>
     *
     * @param queue              Nombre de la cola de evaluación.
     * @param deadLetterExchange Exchange al que derivar lo rechazado.
     * @param deadLetterQueue    Routing key de derivación, igual al nombre de la DLQ.
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
     * Cola donde acaban los envíos que agotaron los reintentos.
     *
     * @param deadLetterQueue Nombre de la cola de mensajes muertos.
     * @return Cola de mensajes muertos de evaluación.
     */
    @Bean
    public Queue submissionEvaluateDeadLetterQueue(
            @Value("${app.messaging.submission.dead-letter-queue}") String deadLetterQueue) {
        return QueueBuilder.durable(deadLetterQueue).build();
    }

    /**
     * Enlaza la cola de evaluación con el exchange principal, con la misma routing
     * key que publica {@code submission-service}.
     *
     * @param submissionEvaluateQueue Cola de evaluación.
     * @param submissionExchange      Exchange principal.
     * @param routingKey              Routing key de los envíos pendientes de evaluar.
     * @return Binding de la cola de evaluación.
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
     * Enlaza la DLQ de evaluación con el exchange de mensajes muertos.
     *
     * @param submissionEvaluateDeadLetterQueue Cola de mensajes muertos de evaluación.
     * @param submissionDeadLetterExchange      Exchange de mensajes muertos.
     * @param deadLetterQueue                   Routing key de derivación.
     * @return Binding de la DLQ de evaluación.
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
     * Cola en la que aterriza el veredicto que publica
     * {@code RabbitJudgeResultPublisherAdapter}.
     * <p>
     * Sin ella, el mensaje llegaría al exchange sin cola que lo acepte. Como el
     * publicador envía con {@code mandatory} y comprueba el retorno, eso no se
     * manifestaría como un veredicto perdido sino como un
     * {@code ResultPublishingException}: es la cola que hace observable el tramo
     * de vuelta.
     * </p>
     *
     * @param judgedQueue           Nombre de la cola de veredictos.
     * @param deadLetterExchange    Exchange al que derivar lo rechazado.
     * @param judgedDeadLetterQueue Routing key de derivación, igual al nombre de su DLQ.
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
     * Cola donde acaban los veredictos que no pudieron registrarse.
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
     * Enlaza la cola de veredictos con el exchange principal, con la misma routing
     * key que usa el publicador de este servicio.
     *
     * @param submissionJudgedQueue Cola de veredictos.
     * @param submissionExchange    Exchange principal.
     * @param judgedRoutingKey      Routing key de los veredictos.
     * @return Binding de la cola de veredictos.
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
     * Enlaza la DLQ de veredictos con el exchange de mensajes muertos.
     *
     * @param submissionJudgedDeadLetterQueue Cola de mensajes muertos de veredictos.
     * @param submissionDeadLetterExchange    Exchange de mensajes muertos.
     * @param judgedDeadLetterQueue           Routing key de derivación.
     * @return Binding de la DLQ de veredictos.
     */
    @Bean
    public Binding submissionJudgedDeadLetterBinding(Queue submissionJudgedDeadLetterQueue,
            DirectExchange submissionDeadLetterExchange,
            @Value("${app.messaging.submission.judged-dead-letter-queue}") String judgedDeadLetterQueue) {
        return BindingBuilder.bind(submissionJudgedDeadLetterQueue)
                .to(submissionDeadLetterExchange)
                .with(judgedDeadLetterQueue);
    }
}
