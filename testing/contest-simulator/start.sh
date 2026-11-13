#!/bin/bash
# Arranca lo necesario y abre el simulador de concurso en http://localhost:4400:
#   contest-platform (:8080), si no esta ya corriendo, y el simulador.
# Los envios los califica el judge-service real, asi que RabbitMQ (:5672) es obligatorio y el
# contenedor goslint-judge debe estar corriendo (en el contexto nativo de Docker: --context default).
# Solo detiene al salir (Ctrl+C) el servicio que arranco el. Postgres debe estar corriendo.
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
    if listening "$port"; then echo "[simulador] $name ya corre en :$port"; return; fi
    if [ ! -f "$jar" ] || [ -n "$(find "$BACKEND/services/$name/src" -newer "$jar" -print -quit)" ]; then
        echo "[simulador] compilando $name..."
        (cd "$BACKEND" && ./gradlew ":services:$name:bootJar" -q) || { echo "[simulador] falló la compilación de $name"; exit 1; }
    fi
    SERVER_PORT="$port" java -jar "$jar" > "$LOGS/$name.log" 2>&1 &
    PIDS+=($!)
    echo "[simulador] iniciando $name en :$port (log: backend/logs/$name.log)"
    for _ in $(seq 1 60); do listening "$port" && return; sleep 1; done
    echo "[simulador] $name no respondió en 60 s; revisa backend/logs/$name.log"; exit 1
}

# El puerto sale del .env: en local RabbitMQ puede no estar en el 5672 por defecto.
RABBIT_PORT="${RABBITMQ_PORT:-5672}"
listening "$RABBIT_PORT" || { echo "[simulador] RabbitMQ no responde en :$RABBIT_PORT; sin él los envíos no llegan al juez."; exit 1; }

# El juez solo funciona en el Docker nativo (necesita la slice de cgroups del host).
if command -v docker >/dev/null 2>&1 && ! docker --context default ps --format '{{.Names}}' 2>/dev/null | grep -qx goslint-judge; then
    if docker --context default ps -a --format '{{.Names}}' 2>/dev/null | grep -qx goslint-judge \
        && docker --context default start goslint-judge >/dev/null 2>&1; then
        echo "[simulador] goslint-judge estaba parado; arrancado."
    else
        echo "[simulador] AVISO: goslint-judge no corre y no se pudo arrancar; los envíos se quedarán en cola sin veredicto."
        echo "[simulador]        ¿Está activa la slice? systemctl is-active goslint.slice"
    fi
fi

start_service contest-platform 8080
node server.js
