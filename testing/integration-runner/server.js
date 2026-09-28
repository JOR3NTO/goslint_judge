"use strict";

const path = require("path");
require("dotenv").config({ path: path.join(__dirname, "..", "..", "backend", "services", "submission-service", ".env") });

const express = require("express");
const cors    = require("cors");
const fetch   = require("node-fetch");

const HTTP_PORT      = process.env.INTEGRATION_PORT   || 4200;
const AUTH_URL       = `http://localhost:${process.env.AUTH_PORT       || 8081}`;
const PROBLEM_URL    = `http://localhost:${process.env.PROBLEM_PORT    || 8082}`;
const SUBMISSION_URL = `http://localhost:${process.env.SUBMISSION_PORT || 8083}`;
const JUDGE_URL      = `http://localhost:${process.env.JUDGE_PORT      || 8084}`;

async function proxyRequest(req, res, targetUrl) {
  try {
    const headers = { "Content-Type": "application/json" };
    if (req.headers.authorization) headers["Authorization"] = req.headers.authorization;

    const opts = { method: req.method, headers };
    if (req.method !== "GET" && req.method !== "HEAD") {
      opts.body = JSON.stringify(req.body);
    }

    const upstream = await fetch(targetUrl, opts);
    const text = await upstream.text();
    res.status(upstream.status).type("json").send(text);
  } catch (err) {
    res.status(502).json({ error: `No se pudo conectar al servicio: ${err.message}` });
  }
}

async function pingUrl(url) {
  try {
    const r = await fetch(url, { signal: AbortSignal.timeout(2500) });
    return r.status < 500 ? "up" : "degraded";
  } catch (e) {
    if (e.name === "TimeoutError" || e.name === "AbortError") return "timeout";
    return "down";
  }
}

const app = express();
app.use(cors());
app.use(express.json({ limit: "2mb" }));
app.use(express.static(path.join(__dirname, "public")));

// ── Health ────────────────────────────────────────────────────────────────────
app.get("/api/health", async (_req, res) => {
  const [auth, problem, submission, judge] = await Promise.all([
    pingUrl(`${AUTH_URL}/api/v1/auth/login`).catch(() => "down"),
    pingUrl(`${PROBLEM_URL}/api/v1/problems/all`).catch(() => "down"),
    pingUrl(`${SUBMISSION_URL}/api/v1/submissions`).catch(() => "down"),
    pingUrl(`${JUDGE_URL}/actuator/health`).catch(() => "down"),
  ]);
  res.json({ auth, problem, submission, judge });
});

// ── Auth proxy ────────────────────────────────────────────────────────────────
app.post("/api/proxy/auth/register", (req, res) =>
  proxyRequest(req, res, `${AUTH_URL}/api/v1/auth/register`));

app.post("/api/proxy/auth/login", (req, res) =>
  proxyRequest(req, res, `${AUTH_URL}/api/v1/auth/login`));

// ── Problem proxy ─────────────────────────────────────────────────────────────
app.post("/api/proxy/problems", (req, res) =>
  proxyRequest(req, res, `${PROBLEM_URL}/api/v1/problems`));

app.get("/api/proxy/problems/all", (req, res) =>
  proxyRequest(req, res, `${PROBLEM_URL}/api/v1/problems/all`));

app.get("/api/proxy/problems/:id", (req, res) =>
  proxyRequest(req, res, `${PROBLEM_URL}/api/v1/problems/${req.params.id}`));

app.post("/api/proxy/problems/test-cases/:problemId", (req, res) =>
  proxyRequest(req, res, `${PROBLEM_URL}/api/v1/problems/test-cases/${req.params.problemId}`));

// ── Submission proxy ──────────────────────────────────────────────────────────
app.post("/api/proxy/submissions", (req, res) =>
  proxyRequest(req, res, `${SUBMISSION_URL}/api/v1/submissions`));

app.get("/api/proxy/submissions", (req, res) =>
  proxyRequest(req, res, `${SUBMISSION_URL}/api/v1/submissions`));

app.get("/api/proxy/submissions/:id", (req, res) =>
  proxyRequest(req, res, `${SUBMISSION_URL}/api/v1/submissions/${req.params.id}`));

app.listen(HTTP_PORT, () => {
  console.log(`[integration-runner] Panel en http://localhost:${HTTP_PORT}`);
});
