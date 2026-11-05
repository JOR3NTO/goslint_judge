"use strict";

// Probador de equipos e invitaciones de contest-service. Sin dependencias:
// solo http y fetch nativos de Node (>= 20).
const http = require("http");
const fs   = require("fs");
const path = require("path");

try {
  process.loadEnvFile(path.join(__dirname, "..", "..", "backend", "services", "submission-service", ".env"));
} catch { /* sin .env: el secreto se pega a mano en el panel */ }

const PORT        = Number(process.env.TEAMS_TESTER_PORT || 4300);
const CONTEST_URL = (process.env.CONTEST_URL || "http://localhost:8086").replace(/\/+$/, "");
const PROBLEM_URL = (process.env.PROBLEM_URL || "http://localhost:8082").replace(/\/+$/, "");
const SUBMISSION_URL = (process.env.SUBMISSION_URL || "http://localhost:8083").replace(/\/+$/, "");
const PUBLIC_DIR  = path.join(__dirname, "public");

// Sirve el JWT_SECRET al navegador para firmar tokens de prueba: solo en local.
const CONFIG = {
  jwtSecret:  process.env.JWT_SECRET || "",
  jwtIssuer:  process.env.JWT_ISSUER || "goslint-judge",
  contestUrl: CONTEST_URL,
  problemUrl: PROBLEM_URL,
  submissionUrl: SUBMISSION_URL,
};

const TYPES = { ".html": "text/html; charset=utf-8", ".js": "text/javascript; charset=utf-8", ".css": "text/css; charset=utf-8" };

function sendJson(res, status, body) {
  res.writeHead(status, { "Content-Type": "application/json; charset=utf-8" });
  res.end(JSON.stringify(body));
}

// Reenvía /svc/* a contest-service, /ssvc/* a submission-service y /psvc/* (solo lectura) a problem-service.
async function proxy(req, res, baseUrl, prefix) {
  const chunks = [];
  for await (const c of req) chunks.push(c);
  const headers = {};
  if (req.headers["content-type"])  headers["Content-Type"]  = req.headers["content-type"];
  if (req.headers.authorization)    headers["Authorization"] = req.headers.authorization;
  try {
    const upstream = await fetch(baseUrl + req.url.slice(prefix.length), {
      method: req.method,
      headers,
      body: ["GET", "HEAD"].includes(req.method) ? undefined : Buffer.concat(chunks),
    });
    res.writeHead(upstream.status, { "Content-Type": upstream.headers.get("content-type") || "text/plain" });
    res.end(Buffer.from(await upstream.arrayBuffer()));
  } catch (err) {
    sendJson(res, 502, { message: `No se pudo conectar a ${baseUrl}: ${err.message}` });
  }
}

function serveStatic(req, res) {
  const urlPath = decodeURIComponent(req.url.split("?")[0]);
  const file = path.resolve(PUBLIC_DIR, "." + (urlPath === "/" ? "/index.html" : urlPath));
  if (!file.startsWith(PUBLIC_DIR + path.sep)) return sendJson(res, 403, { message: "Ruta no permitida" });
  fs.readFile(file, (err, data) => {
    if (err) return sendJson(res, 404, { message: "No encontrado" });
    res.writeHead(200, { "Content-Type": TYPES[path.extname(file)] || "application/octet-stream" });
    res.end(data);
  });
}

http.createServer((req, res) => {
  if (req.url === "/favicon.ico") { res.writeHead(204); return res.end(); }
  if (req.url === "/api/config") return sendJson(res, 200, { ...CONFIG, jwtSecretPresent: CONFIG.jwtSecret !== "" });
  if (req.url.startsWith("/svc/")) return proxy(req, res, CONTEST_URL, "/svc");
  if (req.url.startsWith("/ssvc/")) return proxy(req, res, SUBMISSION_URL, "/ssvc");
  if (req.url.startsWith("/psvc/")) {
    if (req.method !== "GET") return sendJson(res, 405, { message: "problem-service es de solo lectura desde el probador" });
    return proxy(req, res, PROBLEM_URL, "/psvc");
  }
  serveStatic(req, res);
}).listen(PORT, "127.0.0.1", () => {
  console.log(`Probador de equipos e invitaciones: http://localhost:${PORT}  (contest-service: ${CONTEST_URL}, problem-service: ${PROBLEM_URL}, submission-service: ${SUBMISSION_URL})`);
});
