# Probador manual del WebSocket de `submission-service`

`judge-service` todavía no existe. Esta carpeta simula su rol para poder ver en
vivo, desde el navegador, cómo cambia el estado de un envío por el canal
`/ws/submissions` — con control manual de cuándo "termina" de evaluar cada uno.

## Piezas

- **`server.js`** — hace de `judge-service`: consume la cola real
  `submission.evaluate` de RabbitMQ y, cuando tú lo decides desde la página,
  publica el veredicto en `submission.judged` con el retraso que elijas.
- **`public/`** — la página de prueba (HTML/CSS/JS puro, sin build): genera un
  JWT válido en el propio navegador (HS256, firmado con el mismo secreto de
  `backend/.env`), se conecta al WebSocket, deja crear varios envíos, y
  muestra tarjetas con el estado de cada uno en tiempo real.

## Requisitos previos

1. Postgres y RabbitMQ corriendo (`docker compose -f infrastructure/docker/docker-compose.yml up -d`).
2. `submission-service` arrancado con las variables de `backend/.env`
   (es quien declara la topología de RabbitMQ que este simulador consume, y
   quien recibe los envíos por `POST /api/v1/submissions`):
   ```bash
   set -a && source backend/.env && set +a
   ./gradlew :services:submission-service:bootRun
   ```
   `backend/.env` trae `AUTH_BYPASS=true`, necesario para que ese POST
   funcione hoy sin un filtro JWT real para peticiones HTTP (ver
   `TemporaryAuthBypassFilter`).

## Uso

```bash
cd testing/ws-judge-simulator
npm install
npm start
```

Abre **http://localhost:4100** (no abras `index.html` directo con `file://`:
`crypto.subtle` para firmar el JWT solo funciona en un contexto servido).

1. **Conexión**: pega el `JWT_SECRET` de `backend/.env`, genera un `userId` y
   pulsa "Conectar".
2. **Crear envíos**: pulsa "Crear vía API real" — llama de verdad a
   `POST /api/v1/submissions`. El campo `teamId` debe coincidir con el
   `userId` conectado (hay un botón "= userId") porque el equipo se resuelve
   hoy como individual.
3. **Juez simulado**: cada envío que llega a la cola aparece en la sección 3.
   Elige veredicto, tiempo de ejecución/memoria y cuánto debe "tardar" el
   juez, y pulsa "Enviar veredicto" — o "Aleatorio" para no pensarlo. También
   hay un modo automático para generar varios veredictos sin intervención.
4. **Tarjetas en vivo**: cambian de color según el estado real que llega por
   el WebSocket (en cola → juzgando → veredicto).
