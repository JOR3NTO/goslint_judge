package co.uceva.judge.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.test.context.SpringBootTest;

import co.uceva.judge.application.port.out.SandboxExecutor;
import co.uceva.judge.infrastructure.sandbox.worker.JudgeWorkerPool;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprueba que el número de workers se ajusta solo con configuración y que
 * arrastra el número de consumidores de RabbitMQ: si divergieran, habría
 * consumidores esperando worker con un envío ya retirado de la cola, o workers
 * que nunca reciben trabajo.
 */
@SpringBootTest(properties = {
        "JUDGE_WORKERS=3",
        "app.security.jwt.secret=una-clave-compartida-de-al-menos-32-bytes!!",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "management.health.rabbit.enabled=false"
})
class WorkerPoolWiringIntegrationTest {

    @Autowired private SandboxExecutor sandboxExecutor;
    @Autowired private RabbitProperties rabbitProperties;

    @Test
    void shouldEvaluateThroughTheWorkerPool() {
        assertThat(sandboxExecutor).isInstanceOf(JudgeWorkerPool.class);
    }

    @Test
    void shouldHaveOneRabbitConsumerPerConfiguredWorker() {
        assertThat(rabbitProperties.getListener().getSimple().getConcurrency()).isEqualTo(3);
    }
}
