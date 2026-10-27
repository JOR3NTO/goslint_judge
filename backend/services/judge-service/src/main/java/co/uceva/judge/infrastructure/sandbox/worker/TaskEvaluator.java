package co.uceva.judge.infrastructure.sandbox.worker;

import co.uceva.judge.domain.model.JudgeResult;
import co.uceva.judge.domain.model.JudgeTask;
import co.uceva.judge.infrastructure.sandbox.workspace.WorkerEnvironment;

/** Trabajo que un worker ejecuta: evaluar una tarea dentro de su propio entorno. */
@FunctionalInterface
public interface TaskEvaluator {

    /**
     * Evalúa la tarea usando solo los recursos del entorno recibido.
     *
     * @param task        Tarea de evaluación.
     * @param environment Entorno del worker que la ejecuta.
     * @return El resultado con veredicto y métricas.
     */
    JudgeResult evaluate(JudgeTask task, WorkerEnvironment environment);
}
