#!/bin/bash
# Inicia judge-service en Docker con el perfil sandbox.
# Ver: backend/services/judge-service/docs/REQUIREMENTS.md
#
# PASO 1 (una sola vez como root, o en cada reinicio del host):
#   sudo mkdir -p /sys/fs/cgroup/goslint.slice
#   echo "+cpu +memory +pids +io" | sudo tee /sys/fs/cgroup/cgroup.subtree_control
#   echo "+cpu +memory +pids +io" | sudo tee /sys/fs/cgroup/goslint.slice/cgroup.subtree_control
#
# PASO 2 (este script):
#   ./start-judge.sh

set -e
INFRA_DIR="$(cd "$(dirname "$0")/../../infrastructure/docker" && pwd)"
JWT_SECRET="${JWT_SECRET:-1004d399e8881577d2024c2a04ee4eff039afec96ea9a54fff8833d8a140639f}"

log() { echo "[start-judge] $*"; }

# Verificar que goslint.slice existe
if [ ! -d "/sys/fs/cgroup/goslint.slice" ]; then
    log "ERROR: /sys/fs/cgroup/goslint.slice no existe."
    log ""
    log "Ejecuta como root (una sola vez o tras reiniciar el host):"
    log "  sudo mkdir -p /sys/fs/cgroup/goslint.slice"
    log "  echo '+cpu +memory +pids +io' | sudo tee /sys/fs/cgroup/cgroup.subtree_control"
    log "  echo '+cpu +memory +pids +io' | sudo tee /sys/fs/cgroup/goslint.slice/cgroup.subtree_control"
    exit 1
fi

log "goslint.slice encontrada. Iniciando judge-service..."

cd "$INFRA_DIR"
JWT_SECRET="$JWT_SECRET" docker compose --profile sandbox up -d judge-service

log ""
log "judge-service iniciado. Verificar:"
log "  docker logs -f goslint-judge"
log "  curl -s http://localhost:8084/actuator/health"
log ""
log "Smoke test del sandbox (esperar a que el contenedor arranque):"
log "  docker exec -u ubuntu goslint-judge sh /opt/judge/prueba_humo.sh"
