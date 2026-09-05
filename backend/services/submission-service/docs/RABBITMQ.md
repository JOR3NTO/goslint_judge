# Mensajería con RabbitMQ — `submission-service`

> Cómo el servicio entrega los envíos al motor de evaluación y cómo recibe de vuelta el veredicto.
> Documento vivo: actualizar cuando cambie la topología o el contrato con `judge-service`.

---

## Tabla de contenido

1. [Qué resuelve](#1-qué-resuelve)
2. [Topología](#2-topología)
3. [Contrato de mensajes](#3-contrato-de-mensajes)
4. [Flujo de salida — encolar un envío](#4-flujo-de-salida--encolar-un-envío)
5. [Flujo de entrada — recibir el veredicto](#5-flujo-de-entrada--recibir-el-veredicto)
6. [Fallos y colas de mensajes muertos](#6-fallos-y-colas-de-mensajes-muertos)
7. [Reintento de envíos pendientes](#7-reintento-de-envíos-pendientes)
8. [Mapa de archivos](#8-mapa-de-archivos)
9. [Configuración](#9-configuración)
10. [Dependencias para funcionar](#10-dependencias-para-funcionar)
11. [Ejecutar sin broker](#11-ejecutar-sin-broker)
12. [Verificación y pruebas](#12-verificación-y-pruebas)

---

## 1. Qué resuelve

Evaluar código tarda: compilar y ejecutar contra decenas de casos de prueba no cabe dentro de una petición HTTP. El estudiante debe recibir un `201 Created` en cuanto su envío queda registrado, y la evaluación ocurrir después, por su cuenta.

RabbitMQ es lo que separa ambas cosas. `submission-service` **no juzga**: persiste el envío, lo publica en una cola y devuelve el control. `judge-service` consume esa cola a su ritmo y publica el resultado en otra, que este servicio consume para cerrar el ciclo.

La consecuencia importante es que **un envío no se pierde porque el juez esté caído**: el mensaje queda duradero en el broker, y si ni siquiera el broker responde, el envío se queda en estado `PENDING` en la base de datos y un barrido periódico lo reintenta.

---

## 2. Topología

Todo cuelga de un único exchange de tipo *topic* (`submission.exchange`) y un exchange *direct* de mensajes muertos (`submission.dlx`). Las declaraciones son **duraderas**: sobreviven a un reinicio del broker.

```
                     ┌──────────────────────────────┐
   publica           │   submission.exchange        │        consume
 submission-service ─┤        (topic, durable)      ├─▶ submission.evaluate ──▶ judge-service
   rk: submission.   │                              │
       evaluate      │                              │
                     │                              │        consume
      judge-service ─┤                              ├─▶ submission.judged  ──▶ submission-service
   rk: submission.   └──────────────────────────────┘
       judged
                                  │ (mensajes rechazados definitivamente)
                                  ▼
                     ┌──────────────────────────────┐
                     │   submission.dlx             ├─▶ submission.evaluate.dlq ─┐
                     │      (direct, durable)       ├─▶ submission.judged.dlq   ─┤
                     └──────────────────────────────┘                            │
                                                                                 ▼
                                                       ExhaustedSubmissionDeadLetterListener
                                                                → status = SYSTEM_ERROR
```

| Recurso | Nombre por defecto | Declarado por | Consumido por |
|---------|--------------------|----------------|----------------|
| Exchange principal | `submission.exchange` | `RabbitConfig#submissionExchange` | — |
| Exchange de fallidos | `submission.dlx` | `RabbitConfig#submissionDeadLetterExchange` | — |
| Cola de evaluación | `submission.evaluate` | `RabbitConfig#submissionEvaluateQueue` | `judge-service` |
| Cola de veredictos | `submission.judged` | `RabbitConfig#submissionJudgedQueue` | `SubmissionJudgedListener` |
| DLQ de evaluación | `submission.evaluate.dlq` | `RabbitConfig#submissionEvaluateDeadLetterQueue` | `ExhaustedSubmissionDeadLetterListener` |
| DLQ de veredictos | `submission.judged.dlq` | `RabbitConfig#submissionJudgedDeadLetterQueue` | `ExhaustedSubmissionDeadLetterListener` |

Las dos colas de trabajo llevan `deadLetterExchange` + `deadLetterRoutingKey` apuntando a su DLQ, así que un mensaje rechazado definitivamente por su consumidor acaba ahí en lugar de desaparecer.

> **Los nombres no están escritos en el código.** Salen de `app.messaging.submission.*` en [application.properties](../src/main/resources/application.properties): `RabbitConfig` declara la topología a partir de esas claves y el publicador envía a esas mismas claves. Cambiar una mueve ambos lados a la vez. Esas propiedades son también **el contrato con `judge-service`**.

---

## 3. Contrato de mensajes

Los cuerpos son JSON (`Jackson2JsonMessageConverter` sobre el `ObjectMapper` de Spring Boot — necesario porque los eventos llevan `Instant`). Los records viven en `shared/common-domain`, de modo que ambos servicios compilan contra la misma definición.

### Salida — `SubmissionReceivedEvent`
[`shared/common-domain/.../event/SubmissionReceivedEvent.java`](../../../shared/common-domain/src/main/java/co/uceva/shared/domain/event/SubmissionReceivedEvent.java)

```json
{
  "submissionId": "…", "teamId": "…", "problemId": "…",
  "language": "PYTHON",
  "sourceCode": "print(42)",
  "submittedAt": "2026-09-05T18:30:00Z"
}
```

Publicado a `submission.exchange` con routing key `submission.evaluate`. El mensaje va marcado como `PERSISTENT` y lleva el `submissionId` como `messageId`, para que el consumidor pueda descartar entregas duplicadas.

### Entrada — `SubmissionJudgedEvent`
[`shared/common-domain/.../event/SubmissionJudgedEvent.java`](../../../shared/common-domain/src/main/java/co/uceva/shared/domain/event/SubmissionJudgedEvent.java)

```json
{
  "submissionId": "…",
  "verdict": "ACCEPTED",
  "executionTimeMs": 120,
  "memoryUsedKb": 2048,
  "judgedAt": "2026-09-05T18:30:04Z"
}
```

`judge-service` debe publicarlo en `submission.exchange` con routing key `submission.judged`.

---

## 4. Flujo de salida — encolar un envío

```
POST /api/v1/submissions
  │
  ├─ SubmitCodeUseCaseImpl        valida, detecta duplicados, persiste (PENDING/PENDING)
  │     └─ publishEvent(SubmissionPersistedEvent)      ← evento interno de Spring, no AMQP
  │
  ├─ [COMMIT de la transacción]
  │
  ├─ SubmissionEnqueueListener    @TransactionalEventListener(AFTER_COMMIT)
  │     └─ EnqueueSubmissionUseCaseImpl   @Transactional(REQUIRES_NEW)
  │           ├─ RabbitSubmissionEventPublisherAdapter.publishSubmissionReceived(...)
  │           │     └─ espera el ACK del broker (hasta confirm-timeout-ms)
  │           └─ si confirmó → submission.markQueued() + save()   → status = QUEUED
  │               si falló  → se registra y se sale: la fila sigue en PENDING
```

Dos decisiones sostienen este flujo:

**Se toca el broker después del commit, no dentro.** Es el problema clásico de escribir en dos sitios a la vez. Si se publicara dentro de la transacción, un rollback posterior dejaría al juez evaluando un envío que no existe, y un fallo del broker tumbaría un envío que el estudiante ya había hecho bien. Por eso `SubmissionEnqueueListener` escucha en fase `AFTER_COMMIT`, y el encolado abre su **propia** transacción (`REQUIRES_NEW`), ya que la anterior está cerrada.

**La publicación es síncrona respecto a la confirmación.** El adaptador no retorna hasta que RabbitMQ acusa recibo (*publisher confirms*, `spring.rabbitmq.publisher-confirm-type=correlated`). Solo entonces el envío pasa a `QUEUED`. Además, con `mandatory=true` un mensaje que llegó al broker pero no encajó en ninguna cola se **devuelve** y se trata como fallo — es lo que hace visible una topología mal declarada, en vez de dar por encolado algo que nadie recibiría.

---

## 5. Flujo de entrada — recibir el veredicto

```
judge-service ─▶ submission.judged
  │
  ├─ SubmissionJudgedListener      @RabbitListener
  │     └─ UpdateSubmissionVerdictUseCaseImpl   @Transactional
  │           ├─ findById  (no existe → AmqpRejectAndDontRequeueException → DLQ directa)
  │           ├─ updateVerdict(verdict, executionTimeMs, memoryUsedKb)   → status = JUDGED
  │           └─ publishEvent(SubmissionStatusChangedEvent)
  │
  ├─ [COMMIT]
  │
  └─ SubmissionStatusNotificationListener  → WebSocket   (ver WEBSOCKET.md)
```

El listener no decide nada: traduce el mensaje a un comando y delega. La distinción que sí hace es entre **fallo transitorio** y **mensaje imposible**:

- La base de datos no responde → la excepción se propaga, el contenedor reintenta (3 intentos con backoff exponencial), y si se agotan el mensaje va a `submission.judged.dlq`.
- El envío referido no existe → `AmqpRejectAndDontRequeueException` inmediata. Reintentarlo no lo haría aparecer; gastar tres intentos sería tiempo perdido.

---

## 6. Fallos y colas de mensajes muertos

`spring.rabbitmq.listener.simple.default-requeue-rejected=false` es imprescindible: sin él, un mensaje rechazado volvería a la misma cola y giraría indefinidamente entre cola y consumidor sin llegar nunca a la DLQ.

[`ExhaustedSubmissionDeadLetterListener`](../src/main/java/co/uceva/submission/infrastructure/messaging/ExhaustedSubmissionDeadLetterListener.java) vigila **ambas** DLQ, porque la evaluación puede romperse en cualquiera de sus dos tramos:

| Cola | Qué pasó | Acción |
|------|----------|--------|
| `submission.evaluate.dlq` | El juez agotó los reintentos sin poder evaluar | `status = SYSTEM_ERROR` |
| `submission.judged.dlq` | El veredicto llegó pero no pudo registrarse | `status = SYSTEM_ERROR` |

Sin este listener, esos envíos quedarían retenidos en el broker y el estudiante seguiría viendo indefinidamente un envío «en cola» que nadie va a evaluar. `SYSTEM_ERROR` es un estado **terminal** que no dice nada sobre la corrección del código (su `verdict` sigue siendo `PENDING`): dice que la plataforma no consiguió emitir un veredicto. Y como cualquier cambio de estado, se notifica por WebSocket, que es lo que cierra la espera.

Este listener **no propaga ningún fallo**: volver a encolar un mensaje en su propia cola de fallidos solo produciría un bucle.

---

## 7. Reintento de envíos pendientes

[`PendingSubmissionRetryScheduler`](../src/main/java/co/uceva/submission/infrastructure/messaging/PendingSubmissionRetryScheduler.java) es la red de seguridad del paso 3 del flujo de salida.

Cada `app.submission.retry.interval-ms` (30 s) busca envíos en `PENDING` con más de `grace-period-ms` (15 s) de antigüedad y vuelve a encolarlos, hasta `batch-size` (50) por ciclo. El periodo de gracia deja fuera los envíos recién registrados, que aún están siendo entregados por el flujo normal, para no duplicar trabajo.

Que el estado viva **en la tabla `submissions`** y no en memoria es justo lo que hace posible esto: el barrido también cubre el caso de que el servicio se detenga entre el registro del envío y su entrega. Al arrancar de nuevo, recoge el trabajo pendiente. La consulta se apoya en el índice parcial que crea la migración `V2`.

Requiere `@EnableScheduling`, declarado en `RabbitConfig`.

---

## 8. Mapa de archivos

| Archivo | Rol |
|---------|-----|
| [infrastructure/config/RabbitConfig.java](../src/main/java/co/uceva/submission/infrastructure/config/RabbitConfig.java) | Declara exchanges, colas, bindings, `RabbitTemplate` y el convertidor JSON |
| [infrastructure/messaging/RabbitSubmissionEventPublisherAdapter.java](../src/main/java/co/uceva/submission/infrastructure/messaging/RabbitSubmissionEventPublisherAdapter.java) | Publica y espera la confirmación del broker |
| [infrastructure/messaging/NoOpSubmissionEventPublisherAdapter.java](../src/main/java/co/uceva/submission/infrastructure/messaging/NoOpSubmissionEventPublisherAdapter.java) | Sustituto sin broker (`app.messaging.enabled=false`) |
| [infrastructure/messaging/SubmissionEnqueueListener.java](../src/main/java/co/uceva/submission/infrastructure/messaging/SubmissionEnqueueListener.java) | Dispara el encolado tras el commit |
| [infrastructure/messaging/SubmissionJudgedListener.java](../src/main/java/co/uceva/submission/infrastructure/messaging/SubmissionJudgedListener.java) | Consume `submission.judged` |
| [infrastructure/messaging/ExhaustedSubmissionDeadLetterListener.java](../src/main/java/co/uceva/submission/infrastructure/messaging/ExhaustedSubmissionDeadLetterListener.java) | Consume ambas DLQ y cierra el envío como `SYSTEM_ERROR` |
| [infrastructure/messaging/PendingSubmissionRetryScheduler.java](../src/main/java/co/uceva/submission/infrastructure/messaging/PendingSubmissionRetryScheduler.java) | Barrido periódico de envíos sin encolar |
| [infrastructure/mapper/SubmissionEventMapper.java](../src/main/java/co/uceva/submission/infrastructure/mapper/SubmissionEventMapper.java) | `Submission` → `SubmissionReceivedEvent` |
| [application/port/out/SubmissionEventPublisher.java](../src/main/java/co/uceva/submission/application/port/out/SubmissionEventPublisher.java) | Puerto de salida (la capa de aplicación no conoce AMQP) |
| [application/usecase/impl/EnqueueSubmissionUseCaseImpl.java](../src/main/java/co/uceva/submission/application/usecase/impl/EnqueueSubmissionUseCaseImpl.java) | Publica y marca `QUEUED` |
| [application/usecase/impl/UpdateSubmissionVerdictUseCaseImpl.java](../src/main/java/co/uceva/submission/application/usecase/impl/UpdateSubmissionVerdictUseCaseImpl.java) | Registra el veredicto y señala el cambio |
| [application/usecase/impl/MarkSubmissionSystemErrorUseCaseImpl.java](../src/main/java/co/uceva/submission/application/usecase/impl/MarkSubmissionSystemErrorUseCaseImpl.java) | Cierra el envío como `SYSTEM_ERROR` |
| [application/exception/EventPublishingException.java](../src/main/java/co/uceva/submission/application/exception/EventPublishingException.java) | Fallo de publicación, traducido fuera de la infraestructura |

---

## 9. Configuración

Todo en [src/main/resources/application.properties](../src/main/resources/application.properties).

### Conexión

| Propiedad | Por defecto | Variable de entorno |
|-----------|-------------|---------------------|
| `spring.rabbitmq.host` | `localhost` | `RABBITMQ_HOST` |
| `spring.rabbitmq.port` | `5672` | `RABBITMQ_PORT` |
| `spring.rabbitmq.username` | `guest` | `RABBITMQ_USERNAME` |
| `spring.rabbitmq.password` | `guest` | `RABBITMQ_PASSWORD` |

### Garantías de entrega

| Propiedad | Valor | Por qué |
|-----------|-------|---------|
| `spring.rabbitmq.publisher-confirm-type` | `correlated` | Sin confirmaciones no se puede saber si un envío quedó encolado |
| `spring.rabbitmq.publisher-returns` / `template.mandatory` | `true` | Un mensaje no enrutable se devuelve en lugar de descartarse en silencio |
| `spring.rabbitmq.template.retry.*` | 3 intentos, 1 s ×2 hasta 10 s | Primer nivel: cortes breves de conexión |
| `app.messaging.confirm-timeout-ms` | `5000` | Espera máxima del ACK antes de dar la entrega por fallida |

### Topología (contrato con `judge-service`)

| Propiedad | Por defecto |
|-----------|-------------|
| `app.messaging.submission.exchange` | `submission.exchange` |
| `app.messaging.submission.routing-key` | `submission.evaluate` |
| `app.messaging.submission.queue` | `submission.evaluate` |
| `app.messaging.submission.dead-letter-exchange` | `submission.dlx` |
| `app.messaging.submission.dead-letter-queue` | `submission.evaluate.dlq` |
| `app.messaging.submission.judged-routing-key` | `submission.judged` |
| `app.messaging.submission.judged-queue` | `submission.judged` |
| `app.messaging.submission.judged-dead-letter-queue` | `submission.judged.dlq` |

### Consumo y reintento

| Propiedad | Por defecto | Nota |
|-----------|-------------|------|
| `spring.rabbitmq.listener.simple.retry.*` | 3 intentos, 1 s ×2 hasta 10 s | Reintentos del consumidor |
| `spring.rabbitmq.listener.simple.default-requeue-rejected` | `false` | **Crítico**: sin esto no se llega nunca a la DLQ |
| `app.submission.retry.interval-ms` | `30000` | Frecuencia del barrido de pendientes |
| `app.submission.retry.grace-period-ms` | `15000` | Antigüedad mínima para reintentar |
| `app.submission.retry.batch-size` | `50` | Envíos por ciclo |
| `app.messaging.enabled` | `true` | `false` desactiva toda la mensajería |

---

## 10. Dependencias para funcionar

Para que el pipeline funcione de extremo a extremo hacen falta:

1. **Un broker RabbitMQ accesible** en `RABBITMQ_HOST:RABBITMQ_PORT`. Con `app.messaging.enabled=true` (el valor por defecto), si el broker no está el servicio **arranca igualmente**, pero cada envío se queda en `PENDING` y el barrido lo reintenta hasta que el broker vuelva.
2. **PostgreSQL con el esquema migrado.** El estado (`PENDING`/`QUEUED`/`JUDGED`/`SYSTEM_ERROR`) vive en la tabla `submissions`; las migraciones `V2` y `V3` de Flyway crean la columna `status`, su `CHECK` y el índice parcial del reintento. Con `ddl-auto=validate`, un esquema desalineado aborta el arranque.
3. **`judge-service` consumiendo `submission.evaluate`** y publicando en `submission.judged`. Sin él los envíos se acumulan en la cola (no se pierden) y nunca pasan de `QUEUED`.
4. **La dependencia Gradle** `spring-boot-starter-amqp`, ya declarada en [build.gradle](../build.gradle).
5. **Los records de `shared/common-domain`** (`SubmissionReceivedEvent`, `SubmissionJudgedEvent`, `VerdictStatus`, `SubmissionStatus`, `ProgrammingLanguage`), compartidos con `judge-service`: son el contrato de serialización.
6. **`@EnableScheduling`** activo (lo aporta `RabbitConfig`) para el barrido de pendientes.

---

## 11. Ejecutar sin broker

`app.messaging.enabled=false` permite levantar el servicio sin RabbitMQ (desarrollo local, pruebas). Con ese valor:

- `RabbitConfig` no se registra → no se declara topología ni se crea `RabbitTemplate`.
- No se registran `SubmissionJudgedListener`, `ExhaustedSubmissionDeadLetterListener` ni `PendingSubmissionRetryScheduler`.
- El bean activo pasa a ser `NoOpSubmissionEventPublisherAdapter`, que da la entrega por confirmada sin contactar con nadie. **No lanza excepción a propósito**: con la mensajería desactivada, dejar los envíos atrapados en `PENDING` y reintentándolos para siempre no aportaría nada.

Conviene excluir además la autoconfiguración AMQP, como hace [application-test.properties](../src/test/resources/application-test.properties):

```properties
app.messaging.enabled=false
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration
```

---

## 12. Verificación y pruebas

| Prueba | Qué cubre |
|--------|-----------|
| [RabbitTopologyConsistencyTest](../src/test/java/co/uceva/submission/infrastructure/config/RabbitTopologyConsistencyTest.java) | Que lo declarado y lo publicado apuntan al mismo sitio |
| [RabbitSubmissionEventPublisherAdapterTest](../src/test/java/co/uceva/submission/infrastructure/messaging/RabbitSubmissionEventPublisherAdapterTest.java) | ACK, NACK, timeout y mensaje devuelto |
| [PendingSubmissionRetrySchedulerTest](../src/test/java/co/uceva/submission/infrastructure/messaging/PendingSubmissionRetrySchedulerTest.java) | Barrido de pendientes |
| [EnqueueSubmissionUseCaseImplTest](../src/test/java/co/uceva/submission/application/usecase/impl/EnqueueSubmissionUseCaseImplTest.java) | Que solo se marca `QUEUED` si hubo confirmación |
| [SubmissionVerdictNotificationIntegrationTest](../src/test/java/co/uceva/submission/SubmissionVerdictNotificationIntegrationTest.java) | Veredicto → persistencia → notificación |

Inspección manual con el broker en marcha (consola de gestión en `http://localhost:15672`, `guest`/`guest`):

```bash
# Colas, mensajes listos y consumidores
rabbitmqctl list_queues name messages consumers

# ¿Hay algo atascado en las colas de fallidos?
rabbitmqctl list_queues name messages | grep dlq
```

Un valor distinto de cero en una DLQ significa envíos que acabaron en `SYSTEM_ERROR`: hay que mirar los logs de `ExhaustedSubmissionDeadLetterListener` y de `judge-service`.
