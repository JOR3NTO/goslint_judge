package co.uceva.submission.infrastructure.config;

import co.uceva.shared.domain.event.SubmissionReceivedEvent;
import co.uceva.submission.domain.model.Submission;
import co.uceva.submission.fixtures.SubmissionFixtures;
import co.uceva.submission.infrastructure.messaging.RabbitSubmissionEventPublisherAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Comprueba que la topología que {@link RabbitConfig} declara y la dirección a la
 * que publica {@code RabbitSubmissionEventPublisherAdapter} salen de las mismas
 * propiedades, de modo que no puedan desalinearse.
 * <p>
 * El contexto se levanta con {@link ApplicationContextRunner}: solo se cargan
 * {@code RabbitConfig} y el publicador, sin base de datos y sin broker.
 * </p>
 * <p>
 * Los valores usados son deliberadamente distintos de los de
 * {@code application.properties}: si alguien vuelve a fijar un nombre en el código,
 * la prueba falla en vez de pasar por coincidencia.
 * </p>
 */
class RabbitTopologyConsistencyTest {

    private static final String EXCHANGE = "custom.exchange";
    private static final String ROUTING_KEY = "custom.routing-key";
    private static final String QUEUE = "custom.queue";
    private static final String DEAD_LETTER_EXCHANGE = "custom.dlx";
    private static final String DEAD_LETTER_QUEUE = "custom.dlq";
    private static final String JUDGED_ROUTING_KEY = "custom.judged.routing-key";
    private static final String JUDGED_QUEUE = "custom.judged.queue";
    private static final String JUDGED_DEAD_LETTER_QUEUE = "custom.judged.dlq";

    /**
     * Contexto mínimo con la topología declarada.
     * <p>
     * El {@code RabbitTemplate} se sustituye por un simulacro para no necesitar una
     * conexión real; se permite la sobrescritura de definiciones porque es lo que
     * hace que la definición explícita reemplace al {@code @Bean} de
     * {@code RabbitConfig}.
     * </p>
     *
     * @param properties Propiedades de topología con las que arrancar el contexto.
     * @return Runner listo para ejecutar aserciones sobre el contexto.
     */
    private static ApplicationContextRunner topologyRunner(String... properties) {
        return new ApplicationContextRunner()
                .withAllowBeanDefinitionOverriding(true)
                .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
                .withBean("rabbitTemplate", RabbitTemplate.class, () -> mock(RabbitTemplate.class))
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withUserConfiguration(RabbitConfig.class)
                .withPropertyValues(properties);
    }

    private final ApplicationContextRunner contextRunner = topologyRunner(
            "app.messaging.submission.exchange=" + EXCHANGE,
            "app.messaging.submission.routing-key=" + ROUTING_KEY,
            "app.messaging.submission.queue=" + QUEUE,
            "app.messaging.submission.dead-letter-exchange=" + DEAD_LETTER_EXCHANGE,
            "app.messaging.submission.dead-letter-queue=" + DEAD_LETTER_QUEUE,
            "app.messaging.submission.judged-routing-key=" + JUDGED_ROUTING_KEY,
            "app.messaging.submission.judged-queue=" + JUDGED_QUEUE,
            "app.messaging.submission.judged-dead-letter-queue=" + JUDGED_DEAD_LETTER_QUEUE,
            "app.messaging.confirm-timeout-ms=200")
            .withBean(RabbitSubmissionEventPublisherAdapter.class);

    /**
     * La comprobación central: lo que el publicador envía tiene que coincidir con lo
     * que el binding declara, o el mensaje acabaría sin cola que lo reciba.
     */
    @Test
    void shouldPublishToTheExchangeAndRoutingKeyDeclaredInTheTopology() {
        contextRunner.run(context -> {
            RabbitTemplate rabbitTemplate = context.getBean(RabbitTemplate.class);
            stubBrokerConfirm(rabbitTemplate);

            Submission submission = SubmissionFixtures.aSubmission();
            context.getBean(RabbitSubmissionEventPublisherAdapter.class)
                    .publishSubmissionReceived(submission);

            ArgumentCaptor<String> exchange = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> routingKey = ArgumentCaptor.forClass(String.class);
            verify(rabbitTemplate).convertAndSend(exchange.capture(), routingKey.capture(),
                    any(SubmissionReceivedEvent.class), any(MessagePostProcessor.class),
                    any(CorrelationData.class));

            Binding binding = context.getBean("submissionEvaluateBinding", Binding.class);
            assertThat(exchange.getValue())
                    .isEqualTo(context.getBean(TopicExchange.class).getName())
                    .isEqualTo(binding.getExchange());
            assertThat(routingKey.getValue()).isEqualTo(binding.getRoutingKey());
        });
    }

    @Test
    void shouldBindTheEvaluationQueueToTheMainExchange() {
        contextRunner.run(context -> {
            Binding binding = context.getBean("submissionEvaluateBinding", Binding.class);

            assertThat(context.getBean(TopicExchange.class).getName()).isEqualTo(EXCHANGE);
            assertThat(context.getBean("submissionEvaluateQueue", Queue.class).getName()).isEqualTo(QUEUE);
            assertThat(binding.getExchange()).isEqualTo(EXCHANGE);
            assertThat(binding.getDestination()).isEqualTo(QUEUE);
            assertThat(binding.getRoutingKey()).isEqualTo(ROUTING_KEY);
        });
    }

    /**
     * Los mensajes rechazados solo llegan a la cola de mensajes muertos si la política
     * de la cola de evaluación apunta al exchange y la routing key que el otro binding
     * declara.
     */
    @Test
    void shouldRouteRejectedMessagesToTheDeclaredDeadLetterQueue() {
        contextRunner.run(context -> {
            Queue evaluateQueue = context.getBean("submissionEvaluateQueue", Queue.class);
            Binding deadLetterBinding = context.getBean("submissionEvaluateDeadLetterBinding", Binding.class);
            DirectExchange deadLetterExchange = context.getBean(DirectExchange.class);

            assertThat(evaluateQueue.getArguments())
                    .containsEntry("x-dead-letter-exchange", deadLetterExchange.getName())
                    .containsEntry("x-dead-letter-routing-key", deadLetterBinding.getRoutingKey());
            assertThat(deadLetterBinding.getExchange()).isEqualTo(DEAD_LETTER_EXCHANGE);
            assertThat(deadLetterBinding.getDestination()).isEqualTo(DEAD_LETTER_QUEUE);
            assertThat(context.getBean("submissionEvaluateDeadLetterQueue", Queue.class).getName())
                    .isEqualTo(DEAD_LETTER_QUEUE);
        });
    }

    /**
     * Sin valor por defecto, una propiedad ausente o mal escrita impide el arranque en
     * lugar de declarar una topología distinta de la que se publica.
     */
    @Test
    void shouldFailToStartWhenATopologyPropertyIsMissing() {
        topologyRunner(
                "app.messaging.submission.routing-key=" + ROUTING_KEY,
                "app.messaging.submission.queue=" + QUEUE,
                "app.messaging.submission.dead-letter-exchange=" + DEAD_LETTER_EXCHANGE,
                "app.messaging.submission.dead-letter-queue=" + DEAD_LETTER_QUEUE,
                "app.messaging.submission.judged-routing-key=" + JUDGED_ROUTING_KEY,
                "app.messaging.submission.judged-queue=" + JUDGED_QUEUE,
                "app.messaging.submission.judged-dead-letter-queue=" + JUDGED_DEAD_LETTER_QUEUE)
                .run(context -> {
                    assertThat(context).hasFailed();
                    // La clave concreta aparece en la causa raíz del fallo de arranque.
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining("Could not resolve placeholder")
                            .hasStackTraceContaining("app.messaging.submission.exchange");
                });
    }

    /**
     * El tramo de vuelta necesita su propio binding: sin él, los veredictos que
     * publique {@code judge-service} llegarían al exchange y no encajarían en
     * ninguna cola, dejando envíos evaluados que nunca se registran.
     */
    @Test
    void shouldBindTheJudgedQueueToTheMainExchange() {
        contextRunner.run(context -> {
            Binding binding = context.getBean("submissionJudgedBinding", Binding.class);

            assertThat(context.getBean("submissionJudgedQueue", Queue.class).getName()).isEqualTo(JUDGED_QUEUE);
            assertThat(binding.getExchange()).isEqualTo(EXCHANGE);
            assertThat(binding.getDestination()).isEqualTo(JUDGED_QUEUE);
            assertThat(binding.getRoutingKey()).isEqualTo(JUDGED_ROUTING_KEY);
        });
    }

    /**
     * Un veredicto que no consiga registrarse tiene que acabar en su cola de
     * fallidos: es de donde el listener lo recoge para cerrar el envío con estado
     * de error del sistema en lugar de dejarlo esperando para siempre.
     */
    @Test
    void shouldRouteRejectedVerdictsToTheDeclaredDeadLetterQueue() {
        contextRunner.run(context -> {
            Queue judgedQueue = context.getBean("submissionJudgedQueue", Queue.class);
            Binding deadLetterBinding = context.getBean("submissionJudgedDeadLetterBinding", Binding.class);
            DirectExchange deadLetterExchange = context.getBean(DirectExchange.class);

            assertThat(judgedQueue.getArguments())
                    .containsEntry("x-dead-letter-exchange", deadLetterExchange.getName())
                    .containsEntry("x-dead-letter-routing-key", deadLetterBinding.getRoutingKey());
            assertThat(deadLetterBinding.getExchange()).isEqualTo(DEAD_LETTER_EXCHANGE);
            assertThat(deadLetterBinding.getDestination()).isEqualTo(JUDGED_DEAD_LETTER_QUEUE);
            assertThat(context.getBean("submissionJudgedDeadLetterQueue", Queue.class).getName())
                    .isEqualTo(JUDGED_DEAD_LETTER_QUEUE);
        });
    }

    /**
     * Los dos tramos comparten exchange pero no cola: si la routing key de ida y la
     * de vuelta coincidieran, cada envío se entregaría a los dos consumidores y el
     * juez acabaría reevaluando sus propios veredictos.
     */
    @Test
    void shouldKeepTheEvaluationAndVerdictQueuesApart() {
        contextRunner.run(context -> {
            Binding evaluate = context.getBean("submissionEvaluateBinding", Binding.class);
            Binding judged = context.getBean("submissionJudgedBinding", Binding.class);

            assertThat(judged.getExchange()).isEqualTo(evaluate.getExchange());
            assertThat(judged.getRoutingKey()).isNotEqualTo(evaluate.getRoutingKey());
            assertThat(judged.getDestination()).isNotEqualTo(evaluate.getDestination());
        });
    }

    /** Completa la confirmación pendiente para que la publicación no espere al broker. */
    private void stubBrokerConfirm(RabbitTemplate rabbitTemplate) {
        doAnswer(invocation -> {
            CorrelationData correlationData = invocation.getArgument(4);
            correlationData.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).convertAndSend(anyString(), anyString(),
                any(SubmissionReceivedEvent.class), any(MessagePostProcessor.class),
                any(CorrelationData.class));
    }
}
