#!/bin/bash
# Detiene los servicios del host y el judge en Docker.

log() { echo "[stop-services] $*"; }

for svc in auth-service problem-service submission-service; do
    pidfile="/tmp/${svc}.pid"
    if [ -f "$pidfile" ]; then
        pid=$(cat "$pidfile")
        if kill -0 "$pid" 2>/dev/null; then
            kill "$pid"
            log "$svc (PID=$pid) detenido"
        else
            log "$svc ya no corre (PID=$pid)"
        fi
        rm -f "$pidfile"
    else
        log "$svc: no hay .pid en /tmp"
    fi
done

# judge-service en Docker
if docker ps --format '{{.Names}}' | grep -q goslint-judge; then
    docker stop goslint-judge
    log "goslint-judge Docker detenido"
fi
