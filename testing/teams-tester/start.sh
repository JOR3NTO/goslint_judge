#!/bin/bash
# Arranca lo necesario para probar y abre el probador en http://localhost:4300:
#   contest-platform (:8080), que expone auth, problemas, concursos y envios, si no esta
#   ya corriendo, y el probador. Si RabbitMQ (:5672) no esta levantado, el servicio arranca
#   con la mensajeria apagada: los envios se aceptan o rechazan igual, pero no llegan al juez.
# Solo detiene al salir (Ctrl+C) el servicio que arranco el. Postgres debe estar corriendo.
# El correo (MAIL_*) se lee de backend/services/contest-platform/.env, que no se versiona.
cd "$(dirname "$0")"
BACKEND="$(cd ../../backend && pwd)"
LOGS="$BACKEND/logs"
mkdir -p "$LOGS"

# DB_*, JWT_SECRET y JWT_ISSUER son los mismos para todos los servicios en local.
set -a
[ -f "$BACKEND/services/contest-platform/.env" ] && . "$BACKEND/services/contest-platform/.env"
set +a

PIDS=()
trap 'for p in "${PIDS[@]}"; do kill "$p" 2>/dev/null; done' EXIT

# ss no existe en macOS: allí se usa lsof.
listening() {
    if command -v ss >/dev/null 2>&1; then ss -ltn | grep -q ":$1 "
    else lsof -nP -iTCP:"$1" -sTCP:LISTEN >/dev/null 2>&1; fi
}

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

start_service contest-platform 8080
node server.js
