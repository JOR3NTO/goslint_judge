package co.uceva.judge.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprueba que {@link RabbitTestTopology} declara la misma topología que
 * {@code submission-service}, que es quien la declara de verdad en producción.
 * <p>
 * El riesgo que cubre no es que la topología de pruebas esté "mal", sino que se
 * desvíe: una cola declarada con argumentos distintos de los de la cola real hace
 * que el broker responda {@code PRECONDITION_FAILED}, y eso aparecería como un
 * fallo de conexión en cualquier prueba de integración, sin señalar la causa.
 * </p>
 * <p>
 * Los nombres usados son distintos de los de producción a propósito: si alguien
 * fija un nombre en el código en lugar de leerlo de las propiedades, la prueba
 * falla en vez de pasar por coincidencia.
 * </p>
 */
class RabbitTestTopologyTest {

    private static final String EXCHANGE = "otro.exchange";
    private static final String ROUTING_KEY = "otro.evaluate";
    private static final String QUEUE = "otro.queue";
    private static final String DEAD_LETTER_EXCHANGE = "otro.dlx";
    private static final String DEAD_LETTER_QUEUE = "otro.dlq";
    private static final String JUDGED_ROUTING_KEY = "otro.judged.routing-key";
    private static final String JUDGED_QUEUE = "otro.judged.queue";
    private static final String JUDGED_DEAD_LETTER_QUEUE = "otro.judged.dlq";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
            .withUserConfiguration(RabbitTestTopology.class)
            .withPropertyValues(
                    "app.messaging.submission.exchange=" + EXCHANGE,
                    "app.messaging.submission.routing-key=" + ROUTING_KEY,
                    "app.messaging.submission.queue=" + QUEUE,
                    "app.messaging.submission.dead-letter-exchange=" + DEAD_LETTER_EXCHANGE,
                    "app.messaging.submission.dead-letter-queue=" + DEAD_LETTER_QUEUE,
                    "app.messaging.submission.judged-routing-key=" + JUDGED_ROUTING_KEY,
                    "app.messaging.submission.judged-queue=" + JUDGED_QUEUE,
                    "app.messaging.submission.judged-dead-letter-queue=" + JUDGED_DEAD_LETTER_QUEUE);

    /**
     * La cola de la que consume {@code SubmissionEvaluationListener} tiene que
     * quedar enlazada al exchange por el que publica {@code submission-service}, o
     * el listener arranca sobre una cola a la que nunca llega nada.
     */
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
     * Los dos argumentos de mensajes muertos son lo que diferencia esta cola de una
     * declarada a secas. Sin ellos el broker rechaza la declaración contra una cola
     * real, y la política de reintentos deja de derivar nada a la DLQ.
     */
    @Test
    void shouldDeclareTheEvaluationQueueWithItsDeadLetterArguments() {
        contextRunner.run(context -> {
            Queue evaluateQueue = context.getBean("submissionEvaluateQueue", Queue.class);
            Binding deadLetterBinding = context.getBean("submissionEvaluateDeadLetterBinding", Binding.class);

            assertThat(evaluateQueue.isDurable()).isTrue();
            assertThat(evaluateQueue.getArguments())
                    .containsEntry("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                    .containsEntry("x-dead-letter-routing-key", DEAD_LETTER_QUEUE);
            assertThat(context.getBean(DirectExchange.class).getName()).isEqualTo(DEAD_LETTER_EXCHANGE);
            assertThat(deadLetterBinding.getDestination()).isEqualTo(DEAD_LETTER_QUEUE);
            assertThat(deadLetterBinding.getRoutingKey()).isEqualTo(DEAD_LETTER_QUEUE);
        });
    }

    /**
     * El tramo de vuelta: el publicador envía con {@code mandatory}, así que un
     * veredicto sin cola destino no se pierde en silencio, falla la evaluación.
     * Esta cola es la que lo evita y la que hace observable el veredicto.
     */
    @Test
    void shouldBindTheJudgedQueueToTheRoutingKeyThePublisherUses() {
        contextRunner.run(context -> {
            Binding binding = context.getBean("submissionJudgedBinding", Binding.class);
            Queue judgedQueue = context.getBean("submissionJudgedQueue", Queue.class);

            assertThat(judgedQueue.getName()).isEqualTo(JUDGED_QUEUE);
            assertThat(binding.getExchange()).isEqualTo(EXCHANGE);
            assertThat(binding.getDestination()).isEqualTo(JUDGED_QUEUE);
            assertThat(binding.getRoutingKey()).isEqualTo(JUDGED_ROUTING_KEY);
            assertThat(judgedQueue.getArguments())
                    .containsEntry("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                    .containsEntry("x-dead-letter-routing-key", JUDGED_DEAD_LETTER_QUEUE);
        });
    }

    /**
     * Ida y vuelta comparten exchange pero no cola. Si las routing keys coincidieran,
     * el juez recibiría sus propios veredictos y volvería a evaluarlos.
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

    /**
     * Sin valor por defecto, una propiedad ausente impide el arranque en lugar de
     * declarar una topología distinta de la real.
     */
    @Test
    void shouldFailToStartWhenATopologyPropertyIsMissing() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
                .withUserConfiguration(RabbitTestTopology.class)
                .withPropertyValues("app.messaging.submission.exchange=" + EXCHANGE)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining("Could not resolve placeholder");
                });
    }
}
