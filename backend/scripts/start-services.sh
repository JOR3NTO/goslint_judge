#!/bin/bash
# Inicia auth-service, problem-service y submission-service en el host.
# judge-service corre en Docker (ver start-judge.sh).
# Prerequisitos: postgres en 5432, rabbitmq en 5672, redis en 6379.

set -e
BACKEND_DIR="$(cd "$(dirname "$0")/.." && pwd)"

log() { echo "[start-services] $*"; }

load_env() {
    local env_file="$1"
    if [ -f "$env_file" ]; then
        set -a
        # shellcheck disable=SC1090
        source "$env_file"
        set +a
    fi
}

start_service() {
    local name="$1"
    local jar="$2"
    local log_file="$3"

    if [ ! -f "$jar" ]; then
        log "ERROR: $name jar no encontrado en $jar"
        log "Ejecuta primero: cd $BACKEND_DIR && ./gradlew :services:${name}:bootJar"
        return 1
    fi

    log "Iniciando $name (log: $log_file)..."
    # SPRING_FLYWAY_VALIDATE_ON_MIGRATE=false: evita fallos por checksums de migraciones ya aplicadas
    SPRING_FLYWAY_VALIDATE_ON_MIGRATE=false java -jar "$jar" > "$log_file" 2>&1 &
    local pid=$!
    echo $pid > "/tmp/${name}.pid"
    log "$name PID=$pid"
}

mkdir -p "$BACKEND_DIR/logs"

# Cada servicio arranca en un subshell para que su .env no lo herede el
# siguiente: el SERVER_PORT de problem-service hacia que submission-service
# intentara abrir el 8082 y muriera con "Address already in use".

# --- auth-service ---
(
    load_env "$BACKEND_DIR/services/auth-service/.env"
    start_service "auth-service" \
        "$BACKEND_DIR/services/auth-service/build/libs/auth-service-1.0.0.jar" \
        "$BACKEND_DIR/logs/auth-service.log"
)

sleep 3

# --- problem-service ---
(
    load_env "$BACKEND_DIR/services/problem-service/.env"
    start_service "problem-service" \
        "$BACKEND_DIR/services/problem-service/build/libs/problem-service-1.0.0.jar" \
        "$BACKEND_DIR/logs/problem-service.log"
)

sleep 3

# --- submission-service ---
(
    load_env "$BACKEND_DIR/services/submission-service/.env"
    start_service "submission-service" \
        "$BACKEND_DIR/services/submission-service/build/libs/submission-service-1.0.0.jar" \
        "$BACKEND_DIR/logs/submission-service.log"
)

log ""
log "Servicios iniciados. Espera ~15s para que arranquen completamente."
log ""
log "Verificar logs:"
log "  tail -f $BACKEND_DIR/logs/auth-service.log"
log "  tail -f $BACKEND_DIR/logs/problem-service.log"
log "  tail -f $BACKEND_DIR/logs/submission-service.log"
log ""
log "Health checks:"
log "  curl -s http://localhost:8081/actuator/health"
log "  curl -s http://localhost:8082/actuator/health"
log "  curl -s http://localhost:8083/actuator/health"
