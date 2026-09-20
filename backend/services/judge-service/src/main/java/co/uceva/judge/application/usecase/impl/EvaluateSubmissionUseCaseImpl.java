package co.uceva.judge.application.usecase.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import co.uceva.judge.application.port.out.JudgeResultPublisher;
import co.uceva.judge.application.port.out.ProblemLimits;
import co.uceva.judge.application.port.out.ProblemLimitsPort;
import co.uceva.judge.application.port.out.SandboxExecutor;
import co.uceva.judge.application.usecase.EvaluateSubmissionUseCase;
import co.uceva.judge.domain.model.JudgeResult;
import co.uceva.judge.domain.model.JudgeTask;
import co.uceva.judge.domain.model.TestCase;
import co.uceva.judge.domain.repository.TestCaseRepository;
import co.uceva.shared.domain.event.SubmissionReceivedEvent;

/**
 * Servicio de Aplicación que implementa el caso de uso de evaluación de un
 * envío.
 */
@Service
public class EvaluateSubmissionUseCaseImpl implements EvaluateSubmissionUseCase {

    private static final Logger log = LoggerFactory.getLogger(EvaluateSubmissionUseCaseImpl.class);

    private final TestCaseRepository testCaseRepository;
    private final ProblemLimitsPort problemLimitsPort;
    private final SandboxExecutor sandboxExecutor;
    private final JudgeResultPublisher judgeResultPublisher;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param testCaseRepository   Puerto de salida para obtener los casos de prueba.
     * @param problemLimitsPort    Puerto de salida para obtener los límites del problema.
     * @param sandboxExecutor      Puerto de salida que ejecuta la solución en el sandbox.
     * @param judgeResultPublisher Puerto de salida para publicar el resultado.
     */
    public EvaluateSubmissionUseCaseImpl(TestCaseRepository testCaseRepository, ProblemLimitsPort problemLimitsPort,
            SandboxExecutor sandboxExecutor, JudgeResultPublisher judgeResultPublisher) {
        this.testCaseRepository = testCaseRepository;
        this.problemLimitsPort = problemLimitsPort;
        this.sandboxExecutor = sandboxExecutor;
        this.judgeResultPublisher = judgeResultPublisher;
    }

    /**
     * Ejecuta el flujo de evaluación:
     * <ol>
     *   <li>Obtiene los casos de prueba y los límites del problema.</li>
     *   <li>Ensambla la {@link JudgeTask} y la ejecuta en el sandbox.</li>
     *   <li>Publica el resultado hacia {@code submission-service}.</li>
     * </ol>
     * Los fallos del sistema (casos de prueba ausentes, sandbox averiado o
     * publicación fallida) se propagan para que la mensajería reintente el
     * envío y, agotados los reintentos, lo cierre desde la cola de fallidos.
     *
     * @param event Evento con el envío a evaluar.
     * @return El resultado de la evaluación.
     */
    @Override
    public JudgeResult execute(SubmissionReceivedEvent event) {
        List<TestCase> testCases = testCaseRepository.findAllByProblemId(event.problemId());
        ProblemLimits limits = problemLimitsPort.findByProblemId(event.problemId());

        JudgeTask task = JudgeTask.create(event.submissionId(), event.language(), event.sourceCode(), testCases,
                limits.timeLimit().milliseconds(), limits.memoryLimit().kilobytes());

        JudgeResult result = sandboxExecutor.execute(task);
        judgeResultPublisher.publish(result);
        log.info("Envío {} evaluado con veredicto {}.", result.getSubmissionId(), result.getVerdict());
        return result;
    }
}
