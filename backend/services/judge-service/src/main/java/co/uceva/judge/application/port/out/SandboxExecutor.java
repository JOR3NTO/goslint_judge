package co.uceva.judge.application.port.out;

import co.uceva.judge.domain.model.JudgeResult;
import co.uceva.judge.domain.model.JudgeTask;

/**
 * Puerto de salida que ejecuta una tarea de evaluación dentro del sandbox:
 * compila si el lenguaje lo requiere, corre la solución contra los casos de
 * prueba y compara su salida con la esperada.
 */
public interface SandboxExecutor {

    /**
     * Evalúa la tarea y produce su resultado.
     *
     * @param task Tarea de evaluación a ejecutar.
     * @return El resultado con veredicto y métricas.
     * @throws co.uceva.judge.domain.exception.SandboxExecutionException si el sandbox falla por un
     *         problema del sistema y no por el código del estudiante.
     */
    JudgeResult execute(JudgeTask task);
}
