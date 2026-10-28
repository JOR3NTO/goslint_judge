# Workers de evaluación

`judge-service` evalúa varios envíos a la vez con un pool de workers independientes. Este documento explica qué es un worker, cómo se reparten los envíos, qué ocurre cuando uno falla y cómo se configura.

La jerarquía de cgroups que usa cada worker está en [CGROUPS.md](./CGROUPS.md); el aislamiento de cada ejecución, en [BWRAP.md](./BWRAP.md).

---

## 1. Qué es un worker

Un worker es un puesto de evaluación que atiende **un envío cada vez** y que no comparte nada con los demás:

| Recurso | Dónde vive | Para qué |
|---|---|---|
| Hilo | `judge-worker-<n>` | Uno nuevo por evaluación: compila y ejecuta el envío, y aparece en cada línea del log |
| Rama de cgroups | `/cg/worker-<n>` | Padre de las hojas `prog-<uuid>` de sus ejecuciones |
| Directorio | `/work/worker-<n>` | Fuente, binarios compilados y carpeta de cada ejecución |

El pool crea todos los workers al arrancar el servicio y registra cuántos quedaron listos:

```
Pool de evaluación con 4 workers, 4 con su entorno listo (tiempo máximo por evaluación: 300 s).
```

Un worker cuyo entorno no se pudo crear (por ejemplo, porque falta la rama delegada) no tumba el servicio: lo reintenta con cada envío que recibe y, mientras tanto, ese envío falla como error del sistema.

---

## 2. Cómo se reparte un envío

```
submission.evaluate (RabbitMQ)
        │   un consumidor por worker, prefetch=1
        ▼
SubmissionEvaluationListener ──► EvaluateSubmissionUseCase ──► SandboxExecutor
                                                                    │
                                                         JudgeWorkerPool.execute(task)
                                                                    │  1. toma un worker libre
                                                                    │  2. evalúa en el hilo del worker
                                                                    │  3. libera sus recursos (o lo reinicia)
                                                                    ▼
                                                              JudgeWorker ──► RunnerSandboxExecutor ──► Runner
```

- **Asignación.** [JudgeWorkerPool](../src/main/java/co/uceva/judge/infrastructure/sandbox/worker/JudgeWorkerPool.java) implementa el puerto `SandboxExecutor`: busca un worker libre, lo marca como ocupado y ejecuta en él la evaluación. Si no hay ninguno, vuelve a mirar cada 10 ms. El caso de uso no sabe que existen workers.
- **Cola.** Hay tantos consumidores de RabbitMQ como workers (`spring.rabbitmq.listener.simple.concurrency` toma el valor de `app.sandbox.workers.count`) y cada uno retiene un solo mensaje. Con todos los workers ocupados, los envíos nuevos esperan en `submission.evaluate`, en el broker, que es duradero: si el juez se cae, no se pierden.
- **Liberación.** Al terminar, el worker mata y borra las hojas de cgroup que hayan quedado y vacía su directorio antes de volver al pool.

---

## 3. Aislamiento entre evaluaciones

| Qué podría filtrarse | Cómo se evita |
|---|---|
| Procesos que sobreviven al envío | Cada ejecución corre en su hoja y en su propio PID namespace; al terminar la evaluación, el worker escribe `cgroup.kill` en su rama, que mata el subárbol entero |
| Archivos (fuentes, binarios, temporales) | Todo se escribe bajo `/work/worker-<n>` y se vacía tras cada evaluación; lo que el programa escribe en `/work` y `/tmp` es un tmpfs de bwrap que desaparece con él |
| Estado en memoria del juez | `RunnerSandboxExecutor` y `Runner` no guardan estado entre evaluaciones; cada evaluación usa un hilo nuevo y el worker solo guarda si está ocupado y si su entorno está listo |
| Interferencia entre workers | Ningún worker crea ni borra nada fuera de su rama y de su directorio |

---

## 4. Fallos de un worker

Un worker se considera fallido cuando la evaluación:

- lanza una excepción;
- devuelve `JUDGE_ERROR`, que es como el `Runner` informa de un fallo de la plataforma;
- supera `evaluation-timeout-seconds` (worker colgado);
- o termina bien, pero no consigue liberar sus recursos.

En todos los casos el pool **reinicia el worker** (interrumpe el hilo de la evaluación, mata sus procesos, borra su rama y su directorio, y los crea de nuevo) antes de dejarlo libre, de modo que el pool nunca pierde capacidad y los demás workers siguen evaluando sin enterarse. En el log:

```
WARN  JudgeWorkerPool : El worker 2 falló durante una evaluación; se reinicia.
```

El worker colgado se detecta sin nada especial: quien pidió la evaluación espera al hilo con `join(tiempo máximo)` y, si al volver el hilo sigue vivo, lo da por colgado.

Lo que le ocurre al envío no cambia respecto a antes: la excepción llega al listener, que reintenta según `spring.rabbitmq.listener.simple.retry.*` y, agotados los intentos, lo manda a la DLQ para que `submission-service` lo cierre como error del sistema.

---

## 5. Configuración

Todo se cambia con variables de entorno del contenedor, sin tocar el código ni reconstruir la imagen; basta recrear el contenedor.

| Variable | Propiedad | Por defecto | Rol |
|---|---|---|---|
| `JUDGE_WORKERS` | `app.sandbox.workers.count` | `2` | Evaluaciones simultáneas |
| `JUDGE_EVALUATION_TIMEOUT_SECONDS` | `app.sandbox.workers.evaluation-timeout-seconds` | `300` | Tiempo máximo de una evaluación completa antes de dar al worker por colgado |

```bash
JUDGE_WORKERS=6 docker compose --profile sandbox up -d judge-service
```

### Cómo elegir el número de workers

- **CPU.** El tiempo de un envío se mide en CPU de su cgroup, no en tiempo real, así que la medida aguanta bien la concurrencia. Aun así, con más workers que núcleos libres las soluciones compiten entre sí y se acercan al tope de tiempo real (`app.sandbox.monitor.absolute-time-ms`). Como punto de partida: núcleos del host menos uno o dos, que quedan para la JVM del juez y el resto de servicios.
- **Memoria.** Cada evaluación puede llegar al límite de memoria de su problema (256 MB como máximo). `JUDGE_WORKERS` por ese límite, más el propio servicio, debe caber en el host y en el `MemoryMax` de la slice, si se define.
- **`/work`.** El tmpfs del contenedor (`JUDGE_WORK_SIZE`, 256 MB por defecto) guarda a la vez los fuentes y binarios de todos los workers.
- **Tiempo máximo.** Con los tres intentos del consumidor, `evaluation-timeout-seconds` por tres debe quedar por debajo del `consumer_timeout` de RabbitMQ (30 minutos por defecto); si no, el broker cierra el canal antes de que el envío llegue a la DLQ.

---

## 6. Cómo probarlo

El panel de integración incluye una página que lanza muchos envíos a la vez y dibuja, por cada uno, cuánto espera en la cola y cuánto tarda en evaluarse:

```bash
backend/scripts/start-services.sh          # auth, problem y submission
backend/scripts/start-judge.sh             # juez (necesita goslint.slice activa)
testing/integration-runner/start.sh        # panel en http://localhost:4200
```

En <http://localhost:4200/workers.html> se elige el número de envíos, la carga y el lenguaje. La página crea su propio problema, firma sus tokens y, al terminar, comprueba que el pico de evaluaciones simultáneas coincide con `JUDGE_WORKERS` y que cada envío recibió el veredicto esperado.

---

## 7. Mapa de archivos

| Archivo | Rol |
|---|---|
| [sandbox/worker/JudgeWorkerPool.java](../src/main/java/co/uceva/judge/infrastructure/sandbox/worker/JudgeWorkerPool.java) | Pool: lista de workers; marca uno libre como ocupado, lo libera al terminar y reinicia al que falla |
| [sandbox/worker/JudgeWorker.java](../src/main/java/co/uceva/judge/infrastructure/sandbox/worker/JudgeWorker.java) | Un worker: su entorno, el hilo de cada evaluación y la espera con tiempo máximo |
| [sandbox/worker/TaskEvaluator.java](../src/main/java/co/uceva/judge/infrastructure/sandbox/worker/TaskEvaluator.java) | Contrato de la evaluación que ejecuta un worker |
| [sandbox/workspace/WorkerEnvironment.java](../src/main/java/co/uceva/judge/infrastructure/sandbox/workspace/WorkerEnvironment.java) | Rama de cgroups y directorio de un worker: crear, limpiar y destruir |
| [config/SandboxConfig.java](../src/main/java/co/uceva/judge/infrastructure/config/SandboxConfig.java) | Bean del pool con sus propiedades |
| [docker/entrypoint.sh](../docker/entrypoint.sh) | Borra al arrancar las ramas `worker-*` de una vida anterior |

---

## 8. Estado de la validación

Validado en Manjaro x86_64 (kernel 6.18, 16 núcleos). Las primeras comprobaciones se hicieron con las clases reales del servicio, `bwrap` real y una rama de cgroups v2 delegada, **fuera del contenedor**; las dos últimas, con la imagen del servicio y `goslint.slice` real:

| Comprobación | Resultado |
|---|---|
| 18 envíos mezclados (Python, C y C++) en 4 workers: aceptado, respuesta incorrecta, error de compilación, error en ejecución, tiempo y memoria excedidos | Todos con el veredicto esperado, 1,7 s en total |
| 8 envíos Java a la vez en 4 workers | Todos aceptados |
| 8 envíos de CPU pura: 1 worker frente a 4 | 8,1 s frente a 2,0 s; la CPU medida por envío no cambia (unos 310 ms) |
| Programa que deja un hijo en segundo plano | No queda ningún proceso ni hoja al terminar la evaluación |
| Worker colgado con tiempo máximo de 2 s | Abandonado a los 2,0 s; el otro worker sigue evaluando; sin residuos |
| Apagado del pool | Desaparecen las ramas y los directorios de los workers |
| Flujo completo por RabbitMQ (`submission-service`, cola, juez con 2 workers y WebSocket), con la página de prueba de workers | 8 envíos en los cuatro lenguajes y 12 con todos los veredictos: pico de 2 evaluaciones simultáneas, el resto en cola, veredictos correctos |
| Lo mismo **dentro del contenedor**, con `JUDGE_WORKERS=2` | 12 envíos Java, 8 en los cuatro lenguajes y 12 con todos los veredictos: pico de 2, veredictos correctos, ningún worker reiniciado y sin hojas ni archivos residuales |

Pendiente de validar:

- Un worker colgado y la aceleración frente a un solo worker, **dentro del contenedor** (solo se midieron fuera).
- Carga sostenida durante una maratón y el dimensionamiento de `MemoryMax` y `TasksMax` de la slice.
