package co.uceva.judge.infrastructure.sandbox;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co.uceva.judge.domain.model.MonitorLimits;
import co.uceva.judge.domain.model.TestCase;
import co.uceva.judge.domain.repository.MonitorLimitsRepository;
import co.uceva.judge.domain.valueobject.PidsLimit;
import co.uceva.judge.domain.valueobject.VolumeSizeLimit;
import co.uceva.judge.infrastructure.sandbox.command.BwrapCommandFactory;
import co.uceva.judge.infrastructure.sandbox.execution.TestCaseResult;
import co.uceva.judge.infrastructure.sandbox.execution.TestCaseRunner;
import co.uceva.judge.infrastructure.sandbox.workspace.SandboxWorkspace;
import co.uceva.shared.domain.VerdictStatus;

/**
 * Orquestador de la ejecución de una solución dentro del sandbox. Por cada
 * caso de prueba, crea un {@link SandboxWorkspace}, construye el comando de
 * aislamiento con {@link BwrapCommandFactory} y delega la ejecución en un
 * {@link TestCaseRunner}, agregando las métricas resultantes y determinando
 * el veredicto final (aceptado, tiempo/memoria/salida excedidos, error en
 * tiempo de ejecución o respuesta incorrecta).
 */
public class Runner {

    /** Número máximo de procesos que puede crear la solución de forma simultánea. */
    private final PidsLimit maxPids;
    /** Tamaño máximo del volumen escribible ({@code /work}) dentro del sandbox. */
    private final VolumeSizeLimit maxVolumeSize;
    /** Límites vigentes de los monitores; se consultan en cada ejecución para que un cambio se aplique a la siguiente. */
    private final MonitorLimitsRepository monitorLimitsRepository;

    /**
     * Crea el runner con límites de sandbox fijos y los límites de los monitores
     * leídos, en cada ejecución, del repositorio, para poder parametrizarlos en
     * tiempo de ejecución.
     *
     * @param maxPids                 Número máximo de procesos simultáneos permitidos dentro del sandbox.
     * @param maxVolumeSize           Tamaño máximo del volumen escribible ({@code /work}) dentro del sandbox.
     * @param monitorLimitsRepository Fuente de los límites de salida y de tiempo de los monitores.
     */
    public Runner(PidsLimit maxPids, VolumeSizeLimit maxVolumeSize, MonitorLimitsRepository monitorLimitsRepository) {
        this.maxPids = maxPids;
        this.maxVolumeSize = maxVolumeSize;
        this.monitorLimitsRepository = monitorLimitsRepository;
    }

    /**
     * Ejecuta la solución contra la lista de casos de prueba proporcionada.
     * Para cada caso, prepara un {@link SandboxWorkspace} nuevo, construye el
     * comando de bwrap con {@link BwrapCommandFactory} y lo ejecuta mediante
     * el {@link TestCaseRunner}. Determina el veredicto final según los
     * límites de memoria, tiempo, salida y la comparación con la salida
     * esperada.
     *
     * @param command      Intérprete o comando usado para ejecutar la solución.
     * @param solutionPath Ruta del archivo fuente de la solución a ejecutar.
     * @param testCases    Casos de prueba del problema, en el orden en que deben ejecutarse.
     * @param timeLimit    Límite de tiempo de ejecución, en milisegundos, por caso de prueba.
     * @param memoryLimit  Límite de memoria, en bytes, por caso de prueba.
     * @return Mapa con el veredicto de la ejecución ({@code status}), las métricas
     *         máximas observadas ({@code maxCpuTime}, {@code maxMemoryUsed}) y, si la
     *         solución no fue aceptada, el identificador del caso de prueba en el que
     *         falló ({@code failedTestCase}).
     */
    public Map<String, Object> runSolution(String command, String solutionPath, List<TestCase> testCases,
            long timeLimit, long memoryLimit) {

        MonitorLimits limits = monitorLimitsRepository.find();
        TestCaseRunner testCaseRunner = new TestCaseRunner(limits.outputSize(), limits.errorSize(), limits.hardTimePercent(),
                limits.watchInterval(), limits.absoluteTimeLimit());

        Map<String, Object> result = new HashMap<>();
        boolean testsPassed = true;
        long maxCpuTime = 0;
        long maxMemoryUsed = 0;

        for (TestCase testCase : testCases) {
            SandboxWorkspace workspace = null;
            try {
                workspace = SandboxWorkspace.create(memoryLimit, maxPids, solutionPath);
                List<String> runCommand = BwrapCommandFactory.build(workspace, maxVolumeSize, command);

                TestCaseResult testResult = testCaseRunner.run(workspace, runCommand, testCase.input(), timeLimit);
                maxCpuTime = Math.max(maxCpuTime, testResult.cpuTimeUsec());
                maxMemoryUsed = Math.max(maxMemoryUsed, testResult.memoryUsedKb());

                if (testResult.status() != null) {
                    result.put("status", testResult.status());
                    result.put("failedTestCase", testCase.id());
                    break;
                }
                if (!(testsPassed &= testResult.output().equals(testCase.expectedOutput()))) {
                    result.put("failedTestCase", testCase.id());
                    break;
                }

            } catch (IOException | InterruptedException e) {
                System.out.println("Error running solution: " + e.getMessage());
                result.put("status", VerdictStatus.RUNTIME_ERROR);
                result.put("failedTestCase", testCase.id());
            } finally {
                if (workspace != null) {
                    workspace.cleanup();
                }
            }
        }
        result.put("maxCpuTime", maxCpuTime / 1000);
        result.put("maxMemoryUsed", maxMemoryUsed / 1024);
        if (result.containsKey("status")) {
            return result;
        }
        if (testsPassed) {
            result.put("status", VerdictStatus.ACCEPTED);
        } else {
            result.put("status", VerdictStatus.WRONG_ANSWER);
        }
        return result;
    }
}
