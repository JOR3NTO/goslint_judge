# Notificación en tiempo real por WebSocket — `submission-service`

> Cómo el servicio empuja al estudiante el estado de sus envíos sin que la pantalla haga *polling*.
> Documento vivo: actualizar cuando cambie el contrato del canal o la autenticación.

---

## Tabla de contenido

1. [Qué resuelve](#1-qué-resuelve)
2. [Decisiones de diseño](#2-decisiones-de-diseño)
3. [Conectarse](#3-conectarse)
4. [Contrato del mensaje](#4-contrato-del-mensaje)
5. [Autenticación en el handshake](#5-autenticación-en-el-handshake)
6. [Quién recibe qué](#6-quién-recibe-qué)
7. [Flujo completo de una notificación](#7-flujo-completo-de-una-notificación)
8. [Mapa de archivos](#8-mapa-de-archivos)
9. [Configuración](#9-configuración)
10. [Dependencias para funcionar](#10-dependencias-para-funcionar)
11. [Limitaciones conocidas](#11-limitaciones-conocidas)
12. [Probarlo](#12-probarlo)

---

## 1. Qué resuelve

Un envío se registra en milisegundos, pero su veredicto llega segundos o minutos después, por RabbitMQ (ver [RABBITMQ.md](./RABBITMQ.md)). Entre ambos momentos el estudiante mira una pantalla que dice «en cola».

La alternativa sería que el navegador preguntara cada pocos segundos si ya hay veredicto. El WebSocket invierte la relación: el servidor avisa **cuando** el estado cambia, y la pantalla se actualiza sola. Cero recargas, cero peticiones desperdiciadas.

El canal notifica **todo cambio de estado**, no solo el veredicto: también `SYSTEM_ERROR`, que es lo que evita que un envío averiado se quede eternamente aparentando estar en curso.

---

## 2. Decisiones de diseño

| Aspecto | Decisión | Por qué |
|---------|----------|---------|
| Sentido | **Unidireccional.** El servidor empuja; lo que el cliente envíe se descarta sin interpretarlo | No hay superficie por la que un mensaje entrante desencadene una acción |
| Protocolo | WebSocket **a pelo**, sin STOMP ni broker por encima | STOMP existe para que el cliente se suscriba y publique — precisamente lo que no debe poder hacer aquí |
| Autenticación | JWT validado **en el handshake**, no después | Una conexión que nunca se abre no puede recibir ni un mensaje, ni por una condición de carrera |
| Alcance | Destinatarios derivados del equipo dueño del envío, nunca de lo que diga el cliente | Es la regla que impide ver envíos ajenos |
| Estado de sesiones | En memoria, volátil | Una conexión no sobrevive a un reinicio y no tiene por qué: el estado duradero está en la BD |
| Carga útil | Sin código fuente | Ya está en poder de quien lo envió; no tiene por qué recorrer la red otra vez |

Los mensajes que llegan del cliente **se descartan sin leerlos, en lugar de cerrar la conexión**: un cliente que envía algo por error, o un intermediario que inyecta un latido, no deberían costarle al estudiante el canal por el que espera su veredicto.

---

## 3. Conectarse

**Endpoint:** `ws://localhost:8083/ws/submissions` (en producción siempre `wss://`).

Los navegadores no permiten fijar cabeceras al abrir un WebSocket, así que el token viaja como **subprotocolo**. El cliente anuncia dos y el servidor confirma solo el fijo:

```js
const ws = new WebSocket(
  "wss://…/ws/submissions",
  ["goslint-judge", `bearer.${token}`]
);

ws.onmessage = (e) => {
  const evt = JSON.parse(e.data);
  if (evt.type === "SUBMISSION_STATUS_UPDATED") {
    actualizarTarjeta(evt.submissionId, evt.status, evt.verdict);
  }
};
```

El servidor devuelve `goslint-judge` y **nunca** el subprotocolo que contiene el token: el protocolo obliga a confirmar uno de los ofrecidos, y devolver ese lo expondría en la respuesta.

Un handshake sin token válido se responde con **`401`** y el canal no llega a abrirse. El motivo del rechazo se registra en el log pero no se devuelve al cliente: detallar por qué falla un token solo ayudaría a quien está intentando adivinar uno válido.

---

## 4. Contrato del mensaje

Un solo tipo de mensaje hoy, [`SubmissionStatusEventDTO`](../src/main/java/co/uceva/submission/infrastructure/websocket/dto/SubmissionStatusEventDTO.java):

```json
{
  "type": "SUBMISSION_STATUS_UPDATED",
  "submissionId": "3f2a…",
  "problemId": "9c1b…",
  "teamId": "77de…",
  "status": "JUDGED",
  "verdict": "ACCEPTED",
  "executionTimeMs": 120,
  "memoryUsedKb": 2048,
  "occurredAt": "2026-09-05T18:30:04Z"
}
```

| Campo | Notas |
|-------|-------|
| `type` | Discriminador. Existe para que el cliente distinga esta notificación de las que se añadan en el futuro sobre el mismo canal, sin deducirlo de la forma del mensaje |
| `status` | `PENDING` · `QUEUED` · `JUDGING` · `JUDGED` · `SYSTEM_ERROR` |
| `verdict` | `PENDING` · `ACCEPTED` · `WRONG_ANSWER` · `TIME_LIMIT_EXCEEDED` · `MEMORY_LIMIT_EXCEEDED` · `RUNTIME_ERROR` · `COMPILATION_ERROR` |

`status` y `verdict` son **ortogonales**: el veredicto dice cómo resultó evaluado el envío, el estado dice dónde está en el flujo. Un `status = SYSTEM_ERROR` conserva `verdict = PENDING`, porque la plataforma falló, no el código del estudiante.

---

## 5. Autenticación en el handshake

[`JwtHandshakeInterceptor`](../src/main/java/co/uceva/submission/infrastructure/websocket/JwtHandshakeInterceptor.java) busca el token en este orden:

1. **Subprotocolo** `bearer.<token>` — el mecanismo recomendado para un cliente de navegador.
2. **Cabecera** `Authorization: Bearer <token>` — para clientes que sí pueden enviarla.
3. **Query param** `?token=<token>` — recurso de compatibilidad.

> ⚠️ El query param tiene una pega conocida: las URL suelen acabar en los registros de acceso de proxies y servidores, con el token dentro. Por eso los tokens deben ser de vida corta y, en producción, la conexión ir siempre sobre TLS.

La validación la hace [`JwtTokenValidator`](../../../shared/common-infrastructure/src/main/java/co/uceva/shared/infrastructure/security/JwtTokenValidator.java), en `shared/common-infrastructure`: verifica **firma, emisor y vigencia** antes de leer ningún claim, y extrae `sub` (id de usuario) y `role`. El bean se declara en [`JwtConfig`](../src/main/java/co/uceva/submission/infrastructure/config/JwtConfig.java) — aparte de `SecurityConfig`, porque son cosas distintas: aquella configura la cadena de filtros HTTP, esto provee la pieza que reconoce a un usuario. Cuando llegue el filtro JWT de los endpoints HTTP usará **este mismo bean**, y ambos lados validarán exactamente igual.

Si el token es válido, la identidad (`AuthenticatedUser`) se deja en los atributos de la sesión bajo la clave `authenticatedUser`, y el handler la recupera desde ahí. Nunca es `null`: una conexión sin usuario autenticado no llega a abrirse.

> **Nota sobre el estado actual del servicio:** el WebSocket es hoy **el único punto que autentica de verdad**. Los endpoints REST aún no tienen filtro JWT; en desarrollo local se usa `TemporaryAuthBypassFilter` (`app.security.bypass-auth=true`), que debe quedar en `false` en cualquier otro entorno.

---

## 6. Quién recibe qué

```
Submission.teamId ──▶ TeamMembershipPort.findTeam() ──▶ [userId, …] ──▶ sesiones abiertas
```

[`NotifySubmissionStatusUseCaseImpl`](../src/main/java/co/uceva/submission/application/usecase/impl/NotifySubmissionStatusUseCaseImpl.java) es donde vive la regla que impide que nadie vea envíos ajenos: los destinatarios se derivan **del equipo dueño del envío y de ninguna otra fuente**. En particular, jamás de nada que el cliente haya podido decir por el canal, que es de un solo sentido.

La composición del equipo se resuelve por el puerto [`TeamMembershipPort`](../src/main/java/co/uceva/submission/application/port/out/TeamMembershipPort.java). Mientras `contest-service` no exista, el adaptador activo es [`NoOpTeamMembershipAdapter`](../src/main/java/co/uceva/submission/infrastructure/client/NoOpTeamMembershipAdapter.java), que trata cada equipo como **individual**: su único integrante es el usuario cuyo id coincide con el del equipo. La suposición es deliberadamente conservadora de cara a la privacidad — nunca amplía la lista más allá de un usuario, así que mientras esté activa es imposible notificar un envío a alguien que no sea su autor.

Cuando `contest-service` publique la composición real, basta con añadir un adaptador que consulte su API y cambiar `app.team-membership.provider`, sin tocar la capa de aplicación.

[`WebSocketSessionRegistry`](../src/main/java/co/uceva/submission/infrastructure/websocket/WebSocketSessionRegistry.java) indexa las conexiones por usuario en un `ConcurrentHashMap`. Un usuario puede tener **varias sesiones a la vez** (varias pestañas, varios dispositivos), de ahí que cada entrada guarde un conjunto. La entrada se elimina al quedarse sin sesiones, para que el mapa no crezca con usuarios que se desconectaron hace rato.

Un destinatario sin conexión abierta simplemente no recibe nada, y un equipo sin integrantes conocidos no es un error: significa que no hay a quién avisar ahora mismo. El estado sigue disponible por HTTP.

---

## 7. Flujo completo de una notificación

```
judge-service ─▶ submission.judged (RabbitMQ)
  │
  ├─ SubmissionJudgedListener
  │     └─ UpdateSubmissionVerdictUseCaseImpl        @Transactional
  │           ├─ updateVerdict(...)  + save()
  │           └─ publishEvent(SubmissionStatusChangedEvent)
  │
  ├─ [COMMIT de la transacción]
  │
  └─ SubmissionStatusNotificationListener            @TransactionalEventListener(AFTER_COMMIT)
        └─ NotifySubmissionStatusUseCaseImpl
              ├─ TeamMembershipPort.findTeam(teamId)  → destinatarios
              └─ WebSocketSubmissionStatusNotifierAdapter.notifyStatusChanged(...)
                    ├─ serializa el DTO una sola vez
                    └─ por cada sesión abierta de cada destinatario → session.sendMessage()
```

La misma cadena la dispara `MarkSubmissionSystemErrorUseCaseImpl` cuando un envío acaba en `SYSTEM_ERROR`.

Dos garantías sostienen el flujo:

**Se notifica después del commit, nunca dentro.** Es lo que hace que la pantalla del estudiante y la base de datos digan siempre lo mismo: si la transacción acabara deshaciéndose, el veredicto ya notificado no existiría en ninguna parte, y no hay forma de retirarlo de una pantalla que no se recarga.

**Un fallo de notificación no propaga.** Se registra y se sigue. El veredicto ya está persistido y accesible por HTTP: no llegar a empujarlo es una molestia, nunca una pérdida de datos. Igual dentro del adaptador: un fallo al escribir en una conexión concreta no impide el envío a las demás.

El adaptador serializa el DTO **una sola vez** para todos los destinatarios, y sincroniza sobre la sesión al escribir: una sesión WebSocket no admite escrituras concurrentes, y dos notificaciones simultáneas al mismo usuario podrían entrelazar sus fragmentos.

---

## 8. Mapa de archivos

| Archivo | Rol |
|---------|-----|
| [infrastructure/websocket/WebSocketConfig.java](../src/main/java/co/uceva/submission/infrastructure/websocket/WebSocketConfig.java) | `@EnableWebSocket`, publica el handler en su ruta y engancha el interceptor |
| [infrastructure/websocket/JwtHandshakeInterceptor.java](../src/main/java/co/uceva/submission/infrastructure/websocket/JwtHandshakeInterceptor.java) | Valida el JWT y rechaza con `401` antes de abrir el canal |
| [infrastructure/websocket/SubmissionStatusWebSocketHandler.java](../src/main/java/co/uceva/submission/infrastructure/websocket/SubmissionStatusWebSocketHandler.java) | Altas y bajas en el registro; descarta todo mensaje entrante |
| [infrastructure/websocket/WebSocketSessionRegistry.java](../src/main/java/co/uceva/submission/infrastructure/websocket/WebSocketSessionRegistry.java) | Índice en memoria de conexiones por usuario |
| [infrastructure/websocket/WebSocketSubmissionStatusNotifierAdapter.java](../src/main/java/co/uceva/submission/infrastructure/websocket/WebSocketSubmissionStatusNotifierAdapter.java) | Serializa y empuja a las sesiones abiertas |
| [infrastructure/websocket/dto/SubmissionStatusEventDTO.java](../src/main/java/co/uceva/submission/infrastructure/websocket/dto/SubmissionStatusEventDTO.java) | Contrato del canal con el frontend |
| [infrastructure/notification/SubmissionStatusNotificationListener.java](../src/main/java/co/uceva/submission/infrastructure/notification/SubmissionStatusNotificationListener.java) | Dispara la notificación tras el commit |
| [infrastructure/client/NoOpTeamMembershipAdapter.java](../src/main/java/co/uceva/submission/infrastructure/client/NoOpTeamMembershipAdapter.java) | Resuelve el equipo como individual mientras no exista `contest-service` |
| [infrastructure/config/JwtConfig.java](../src/main/java/co/uceva/submission/infrastructure/config/JwtConfig.java) | Declara el `JwtTokenValidator` con la clave y el emisor del servicio |
| [application/port/out/SubmissionStatusNotifier.java](../src/main/java/co/uceva/submission/application/port/out/SubmissionStatusNotifier.java) | Puerto de salida: la aplicación no conoce WebSocket |
| [application/port/out/TeamMembershipPort.java](../src/main/java/co/uceva/submission/application/port/out/TeamMembershipPort.java) | Puerto de salida: composición del equipo |
| [application/usecase/impl/NotifySubmissionStatusUseCaseImpl.java](../src/main/java/co/uceva/submission/application/usecase/impl/NotifySubmissionStatusUseCaseImpl.java) | Resuelve destinatarios y notifica |
| [shared/.../security/JwtTokenValidator.java](../../../shared/common-infrastructure/src/main/java/co/uceva/shared/infrastructure/security/JwtTokenValidator.java) | Verificación de firma, emisor y vigencia (compartida) |

---

## 9. Configuración

En [src/main/resources/application.properties](../src/main/resources/application.properties):

| Propiedad | Por defecto | Rol |
|-----------|-------------|-----|
| `app.websocket.submission.path` | `/ws/submissions` | Ruta del canal |
| `app.websocket.submission.sub-protocol` | `goslint-judge` | Subprotocolo que el servidor confirma |
| `app.websocket.allowed-origins` | `*` | Orígenes autorizados a abrir la conexión — **restringir en producción** |
| `app.security.jwt.secret` | *(sin valor)* | `JWT_SECRET`. Clave compartida con `auth-service`, mínimo 32 bytes (HMAC-SHA256) |
| `app.security.jwt.issuer` | `goslint-judge` | `JWT_ISSUER`. Emisor esperado |
| `app.team-membership.provider` | `noop` | Fuente de los integrantes del equipo |

`app.security.jwt.secret` **no tiene valor por defecto a propósito**: un secreto de ejemplo heredado sin querer en producción aceptaría tokens falsificados por cualquiera que conociese el repositorio. Sin la variable de entorno, el servicio no arranca.

---

## 10. Dependencias para funcionar

1. **`spring-boot-starter-websocket`**, ya declarado en [build.gradle](../build.gradle).
2. **`JWT_SECRET` definido**, con al menos 32 bytes y **el mismo** que usa `auth-service` para firmar. Si no coincide, todos los handshakes responden `401`.
3. **`JWT_ISSUER` coincidente** con el emisor de los tokens; un token de otro emisor se rechaza.
4. **Un token ya emitido.** Este canal solo lo consume; emitirlo es cosa de `auth-service`, y siempre por HTTP. Hoy `auth-service` solo expone `/register`, así que la emisión sigue pendiente.
5. **RabbitMQ y `judge-service`** para que haya algo que notificar: el WebSocket no genera eventos, solo transporta los cambios que produce el pipeline de evaluación (ver [RABBITMQ.md](./RABBITMQ.md)). Sin ellos el canal abre y queda en silencio.
6. **PostgreSQL migrado**: la notificación se dispara tras el commit de un cambio de estado, así que si la escritura falla no hay nada que empujar.
7. **`TeamMembershipPort` con un adaptador activo** — hoy `NoOpTeamMembershipAdapter` vía `app.team-membership.provider=noop`.
8. **Que el proxy no corte la conexión.** Traefik u otro reverse proxy delante debe reenviar las cabeceras `Upgrade`/`Connection` y `Sec-WebSocket-Protocol`, o el handshake nunca se completa.

---

## 11. Limitaciones conocidas

- **El registro de sesiones es local a la instancia.** Con varias réplicas de `submission-service`, la instancia que recibe el veredicto por RabbitMQ puede no ser la que tiene abierta la conexión del estudiante, y la notificación se perderá. Escalar horizontalmente requerirá compartir el registro (por ejemplo, un canal de Redis).
- **Sin latido ni reconexión del lado del servidor.** Un proxy que cierre conexiones inactivas dejará al cliente sin canal hasta que reconecte por su cuenta; el frontend debería reintentar.
- **Sin reenvío de lo perdido.** Un cambio de estado ocurrido mientras el cliente estaba desconectado no se recupera al reconectar. La solución es leer el estado por HTTP al abrir la conexión: la BD es la fuente de verdad, el WebSocket solo la adelanta.
- **`allowed-origins=*`** es cómodo en desarrollo y debe restringirse antes de exponer el servicio.

---

## 12. Probarlo

### Pruebas automáticas

| Prueba | Qué cubre |
|--------|-----------|
| [JwtHandshakeInterceptorTest](../src/test/java/co/uceva/submission/infrastructure/websocket/JwtHandshakeInterceptorTest.java) | Extracción del token por las tres vías y rechazo con `401` |
| [SubmissionStatusWebSocketHandlerTest](../src/test/java/co/uceva/submission/infrastructure/websocket/SubmissionStatusWebSocketHandlerTest.java) | Altas, bajas y descarte de mensajes entrantes |
| [WebSocketSessionRegistryTest](../src/test/java/co/uceva/submission/infrastructure/websocket/WebSocketSessionRegistryTest.java) | Varias sesiones por usuario y limpieza del mapa |
| [WebSocketSubmissionStatusNotifierAdapterTest](../src/test/java/co/uceva/submission/infrastructure/websocket/WebSocketSubmissionStatusNotifierAdapterTest.java) | Serialización, sesiones cerradas y fallos aislados |
| [SubmissionVerdictNotificationIntegrationTest](../src/test/java/co/uceva/submission/SubmissionVerdictNotificationIntegrationTest.java) | Veredicto → commit → notificación |

```bash
cd backend
./gradlew :services:submission-service:test
```

### Prueba manual en el navegador

`judge-service` todavía no existe, así que [`testing/ws-judge-simulator/`](../../../../testing/ws-judge-simulator/) simula su papel: consume la cola real `submission.evaluate` y publica el veredicto en `submission.judged` cuando tú lo decides, mostrando en vivo cómo cambian las tarjetas de estado. Ver su [README](../../../../testing/ws-judge-simulator/README.md).
