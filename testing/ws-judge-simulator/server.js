// Simulador manual de judge-service.
//
// judge-service todavia no existe, pero el contrato de mensajeria que
// submission-service ya expone si: publica cada envio en la cola
// "submission.evaluate" y espera un SubmissionJudgedEvent en la routing key
// "submission.judged" del mismo exchange (ver RabbitConfig.java). Este script
// hace de judge-service: consume la cola de evaluacion y, cuando el usuario lo
// decide desde la pagina de prueba, publica el veredicto con el retraso que se
// le indique (para simular el tiempo que tarda el juez real).
//
"use strict";

const path = require("path");
require("dotenv").config({ path: path.join(__dirname, "..", "..", "backend", ".env") });

const express = require("express");
const cors = require("cors");
const amqp = require("amqplib");

const HTTP_PORT = process.env.SIMULATOR_PORT || 4100;

// Mismos nombres que backend/services/submission-service/src/main/resources/application.properties
const EXCHANGE = "submission.exchange";
const EVALUATE_QUEUE = "submission.evaluate";
const JUDGED_ROUTING_KEY = "submission.judged";

const VERDICTS = [
  "ACCEPTED",
  "WRONG_ANSWER",
  "TIME_LIMIT_EXCEEDED",
  "MEMORY_LIMIT_EXCEEDED",
  "RUNTIME_ERROR",
  "COMPILATION_ERROR",
];

const amqpUrl = `amqp://${process.env.RABBITMQ_USERNAME || "guest"}:${
  process.env.RABBITMQ_PASSWORD || "guest"
}@${process.env.RABBITMQ_HOST || "localhost"}:${process.env.RABBITMQ_PORT || 5672}`;

/** @type {Map<string, {submissionId:string, teamId:string, problemId:string, language:string, sourceCode:string, submittedAt:string, receivedAt:number, judging:boolean}>} */
const pending = new Map();

let channel = null;

async function connectAmqp() {
  const connection = await amqp.connect(amqpUrl);
  connection.on("close", () => {
    console.error("[amqp] Conexion cerrada; reintentando en 3s...");
    channel = null;
    setTimeout(connectAmqp, 3000);
  });
  connection.on("error", (err) => console.error("[amqp] Error de conexion:", err.message));

  channel = await connection.createChannel();
  await channel.prefetch(20);

  await channel.consume(
    EVALUATE_QUEUE,
    (msg) => {
      if (!msg) return;
      try {
        const event = JSON.parse(msg.content.toString("utf8"));
        pending.set(event.submissionId, {
          submissionId: event.submissionId,
          teamId: event.teamId,
          problemId: event.problemId,
          language: event.language,
          sourceCode: event.sourceCode,
          submittedAt: event.submittedAt,
          receivedAt: Date.now(),
          judging: false,
        });
        console.log(`[judge-sim] Envio recibido de la cola: ${event.submissionId}`);
      } catch (err) {
        console.error("[judge-sim] Mensaje descartado, no es JSON valido:", err.message);
      }
      channel.ack(msg);
    },
    { noAck: false }
  );

  console.log(`[amqp] Conectado. Escuchando "${EVALUATE_QUEUE}" en ${amqpUrl}`);
}

connectAmqp().catch((err) => {
  console.error("[amqp] No se pudo conectar. ¿Esta corriendo RabbitMQ y ya arranco submission-service " +
    "(es quien declara la topologia)?", err.message);
  setTimeout(connectAmqp, 3000);
});

async function publishJudgedEvent({ submissionId, verdict, executionTimeMs, memoryUsedKb }) {
  if (!channel) throw new Error("Sin conexion a RabbitMQ todavia.");

  const event = {
    submissionId,
    verdict,
    executionTimeMs,
    memoryUsedKb,
    judgedAt: new Date().toISOString(),
  };

  const payload = Buffer.from(JSON.stringify(event), "utf8");
  channel.publish(EXCHANGE, JUDGED_ROUTING_KEY, payload, {
    contentType: "application/json",
    persistent: true,
    messageId: submissionId,
    // DefaultClassMapper de Spring Amqp usa esta cabecera para saber en que
    // clase deserializar el mensaje.
    headers: { __TypeId__: "co.uceva.shared.domain.event.SubmissionJudgedEvent" },
  });

  console.log(`[judge-sim] Veredicto publicado para ${submissionId}: ${verdict}`);
}

const app = express();
app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, "public")));

app.get("/api/health", (_req, res) => {
  res.json({ amqpConnected: Boolean(channel), pendingCount: pending.size });
});

app.get("/api/pending", (_req, res) => {
  res.json([...pending.values()].sort((a, b) => a.receivedAt - b.receivedAt));
});

function scheduleJudgement(id, { verdict, executionTimeMs, memoryUsedKb, delayMs }) {
  const item = pending.get(id);
  if (!item) return { error: 404, message: "Ese envio no esta en la cola simulada del juez." };
  if (item.judging) return { error: 409, message: "Ya hay un veredicto programado para este envio." };
  if (!VERDICTS.includes(verdict)) {
    return { error: 400, message: `Veredicto invalido. Usa uno de: ${VERDICTS.join(", ")}` };
  }

  item.judging = true;
  item.willJudgeAt = Date.now() + delayMs;
  item.verdictScheduled = verdict;

  setTimeout(async () => {
    try {
      await publishJudgedEvent({ submissionId: id, verdict, executionTimeMs, memoryUsedKb });
    } catch (err) {
      console.error("[judge-sim] No se pudo publicar el veredicto:", err.message);
    } finally {
      pending.delete(id);
    }
  }, delayMs);

  return { etaMs: delayMs };
}

app.post("/api/judge/:id", (req, res) => {
  const {
    verdict = "ACCEPTED",
    executionTimeMs = 100,
    memoryUsedKb = 1024,
    delayMs = 2000,
  } = req.body || {};

  const result = scheduleJudgement(req.params.id, { verdict, executionTimeMs, memoryUsedKb, delayMs });
  if (result.error) return res.status(result.error).json({ error: result.message });
  res.status(202).json({ status: "scheduled", etaMs: result.etaMs });
});

app.post("/api/judge/:id/random", (req, res) => {
  const verdict = VERDICTS[Math.floor(Math.random() * VERDICTS.length)];
  const delayMs = 1000 + Math.floor(Math.random() * 4000);
  const executionTimeMs = 50 + Math.floor(Math.random() * 1500);
  const memoryUsedKb = 512 + Math.floor(Math.random() * 30000);

  const result = scheduleJudgement(req.params.id, { verdict, executionTimeMs, memoryUsedKb, delayMs });
  if (result.error) return res.status(result.error).json({ error: result.message });
  res.status(202).json({ status: "scheduled", etaMs: result.etaMs, verdict, executionTimeMs, memoryUsedKb });
});

app.listen(HTTP_PORT, () => {
  console.log(`[judge-sim] Panel de control HTTP en http://localhost:${HTTP_PORT}`);
  console.log(`[judge-sim] Pagina de prueba: http://localhost:${HTTP_PORT}/index.html`);
});
