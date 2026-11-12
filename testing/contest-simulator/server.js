"use strict";

// Simulador de concurso: sirve la página y reenvía /api/* a contest-platform.
// Sin dependencias: solo http, fetch y stream nativos de Node (>= 20).
const http = require("http");
const fs   = require("fs");
const path = require("path");
const { Readable } = require("stream");

const PORT        = Number(process.env.SIMULATOR_PORT || 4400);
const BACKEND_URL = (process.env.BACKEND_URL || "http://localhost:8080").replace(/\/+$/, "");
const PUBLIC_DIR  = path.join(__dirname, "public");

// Sirve el JWT_SECRET al navegador para firmar tokens de prueba: solo en local.
const CONFIG = {
  jwtSecret: process.env.JWT_SECRET || "",
  jwtIssuer: process.env.JWT_ISSUER || "goslint-judge",
  backendUrl: BACKEND_URL,
};

const TYPES = { ".html": "text/html; charset=utf-8", ".js": "text/javascript; charset=utf-8", ".css": "text/css; charset=utf-8" };

function sendJson(res, status, body) {
  res.writeHead(status, { "Content-Type": "application/json; charset=utf-8" });
  res.end(JSON.stringify(body));
}

// Reenvía la respuesta a medida que llega, para que el SSE del scoreboard no quede retenido.
async function proxy(req, res) {
  const chunks = [];
  for await (const c of req) chunks.push(c);
  const headers = {};
  if (req.headers["content-type"]) headers["Content-Type"]  = req.headers["content-type"];
  if (req.headers.authorization)   headers["Authorization"] = req.headers.authorization;
  if (req.headers.accept)          headers["Accept"]        = req.headers.accept;
  const abort = new AbortController();
  res.on("close", () => abort.abort());
  try {
    const upstream = await fetch(BACKEND_URL + req.url, {
      method: req.method,
      headers,
      body: ["GET", "HEAD"].includes(req.method) ? undefined : Buffer.concat(chunks),
      signal: abort.signal,
    });
    res.writeHead(upstream.status, {
      "Content-Type": upstream.headers.get("content-type") || "text/plain",
      "Cache-Control": "no-cache",
    });
    if (!upstream.body) return res.end();
    Readable.fromWeb(upstream.body).on("error", () => res.end()).pipe(res);
  } catch (err) {
    if (abort.signal.aborted) return;
    sendJson(res, 502, { message: `No se pudo conectar a ${BACKEND_URL}: ${err.message}` });
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
  if (req.url === "/config") return sendJson(res, 200, CONFIG);
  if (req.url.startsWith("/api/")) return proxy(req, res);
  serveStatic(req, res);
}).listen(PORT, "127.0.0.1", () => {
  console.log(`Simulador de concurso: http://localhost:${PORT}  (contest-platform: ${BACKEND_URL})`);
});
