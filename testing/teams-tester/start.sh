#!/bin/bash
# Arranca lo necesario para probar y abre el probador en http://localhost:4300:
#   problem-service (:8082) y contest-service (:8086), si no están ya corriendo, y el probador.
# Solo detiene al salir (Ctrl+C) los servicios que arrancó él. Postgres debe estar corriendo.
# El correo (MAIL_*) se lee de backend/services/contest-service/.env, que no se versiona.
cd "$(dirname "$0")"
BACKEND="$(cd ../../backend && pwd)"
LOGS="$BACKEND/logs"
mkdir -p "$LOGS"

# DB_*, JWT_SECRET y JWT_ISSUER son los mismos para todos los servicios en local.
set -a
. "$BACKEND/services/problem-service/.env"
[ -f "$BACKEND/services/contest-service/.env" ] && . "$BACKEND/services/contest-service/.env"
set +a

PIDS=()
trap 'for p in "${PIDS[@]}"; do kill "$p" 2>/dev/null; done' EXIT

listening() { ss -ltn | grep -q ":$1 "; }

start_service() {
    local name="$1" port="$2" jar="$BACKEND/services/$1/build/libs/$1-1.0.0.jar"
    if listening "$port"; then echo "[tester] $name ya corre en :$port"; return; fi
    if [ ! -f "$jar" ] || [ -n "$(find "$BACKEND/services/$name/src" -newer "$jar" -print -quit)" ]; then
        echo "[tester] compilando $name..."
        (cd "$BACKEND" && ./gradlew ":services:$name:bootJar" -q) || { echo "[tester] falló la compilación de $name"; exit 1; }
    fi
    SERVER_PORT="$port" java -jar "$jar" > "$LOGS/$name.log" 2>&1 &
    PIDS+=($!)
    echo "[tester] iniciando $name en :$port (log: backend/logs/$name.log)"
    for _ in $(seq 1 60); do listening "$port" && return; sleep 1; done
    echo "[tester] $name no respondió en 60 s; revisa backend/logs/$name.log"; exit 1
}

start_service problem-service 8082
start_service contest-service 8086

node server.js
