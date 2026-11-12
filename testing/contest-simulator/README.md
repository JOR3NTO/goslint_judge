# Simulador de concurso (`contest-platform` + `judge-service`)

Página para probar el scoreboard con carga realista: crea un concurso con sus problemas y equipos,
y simula a los concursantes enviando código de verdad al `judge-service`. Muestra el scoreboard que llega
por SSE junto al que calcula la propia página con las mismas reglas ICPC, y marca en rojo lo que no coincide.

## Requisitos previos

1. Postgres, RabbitMQ y el `judge-service` corriendo. El juez va en el contexto nativo de Docker:
   `docker --context default ps` debe mostrar `goslint-judge`.
2. `backend/services/contest-platform/.env` con `DB_*`, `JWT_SECRET` y `JWT_ISSUER`.

## Uso

```bash
./testing/contest-simulator/start.sh
```

Arranca `contest-platform` (`:8080`) si no está corriendo (compilándolo si el código cambió) y el simulador.
Sin RabbitMQ no arranca, y avisa si no ve el contenedor del juez. Al salir con Ctrl+C detiene solo lo que
arrancó él. No necesita `npm install`: usa solo módulos nativos de Node 20+.

Abre **http://localhost:4400** (no con `file://`: `crypto.subtle` necesita un contexto servido).

1. **Parámetros**: mínimo 10 problemas y 20 equipos; duración, puntos y penalización de cada problema,
   tiempo que "piensa" cada equipo entre envíos y el porcentaje de cada resultado (AC, WA, TLE, RTE, CE).
2. **Preparar concurso** crea los problemas con sus casos de prueba, el concurso (empieza a los segundos
   indicados), le asigna los problemas e inscribe los equipos, cada uno con un estudiante de UUID aleatorio.
3. **Iniciar simulación** espera al inicio y a que el backend marque el concurso como activo. Cada equipo
   elige un problema, envía y espera el veredicto antes de seguir: tras un AC pasa a otro problema y, si falla,
   lo vuelve a intentar. A veces reenvía un problema ya resuelto, lo que no debe cambiar el scoreboard.

**Congelamiento:** "Congelar últimos (min)" define cuántos minutos antes del fin se congela la tabla pública
(0 = nunca). Al llegar esa hora el backend envía por SSE el evento `frozen` y deja de mandar cambios; la tabla
esperada pasa a contar solo los envíos anteriores al congelamiento. **Liberar scoreboard** hace de organizador
(`POST /contests/{id}/scoreboard/unfreeze`): el backend recalcula a todos los equipos y envía la tabla real.

**Cómo se consigue cada veredicto:** el problema *k* pide imprimir `a*k + b`, así que el simulador sabe qué
código Python da AC, WA (`+ 1`), TLE (bucle infinito), RTE (división entre cero) o CE (error de sintaxis).
La columna "Intento" es lo que se buscó y "Veredicto" lo que respondió el juez; si difieren muchas veces,
el problema está en el juez y no en el scoreboard. Cada envío lleva un comentario único porque el backend
rechaza dos envíos idénticos.

El servidor solo escucha en `127.0.0.1`, porque entrega el `JWT_SECRET` al navegador.
