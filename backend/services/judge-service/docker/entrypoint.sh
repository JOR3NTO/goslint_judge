#!/bin/sh
# entrypoint.sh - Prepara /cg (bind mount de goslint.slice) y baja privilegios.
#
# /cg solo existe en runtime (lo monta docker run / compose), por eso el chown
# NO puede ir en el Dockerfile. Se hace aqui, como root, y despues el contenedor
# corre como usuario sin privilegios.
#
# Variables (solo para pruebas fuera del contenedor):
#   CG_DIR    ruta del cgroup delegado   (por defecto /cg)
#   WORK_DIR  raiz de los directorios por ejecucion (por defecto /work)
#   RUN_USER  usuario al que se bajan privilegios (por defecto ubuntu)
set -eu

CG_DIR="${CG_DIR:-/cg}"
WORK_DIR="${WORK_DIR:-/work}"
RUN_USER="${RUN_USER:-ubuntu}"

log() { echo "[entrypoint] $*" >&2; }

if [ "$(id -u)" -eq 0 ]; then
    if [ -d "$CG_DIR" ]; then
        # 1) Hojas de corridas anteriores. Los cgroups se borran con rmdir
        #    (rm -rf falla porque los archivos de control no se pueden unlink).
        for d in "$CG_DIR"/prog-* "$CG_DIR"/probe-*; do
            if [ -d "$d" ]; then
                rmdir "$d" 2>/dev/null || log "AVISO: no se pudo borrar $d (aun tiene procesos?)"
            fi
        done

        # 2) Delegacion recursiva. El -R es lo importante: mover un proceso a
        #    una hoja exige escritura en cgroup.procs del ancestro comun (/cg).
        #    Debe correr DESPUES de que el host habilite subtree_control.
        chown -R "$RUN_USER:$RUN_USER" "$CG_DIR" 2>/dev/null \
            || log "AVISO: chown -R sobre $CG_DIR fallo en algun archivo"

        # 3) Autodiagnostico: el usuario final debe poder crear una hoja y
        #    mover un proceso a ella. Es exactamente lo que hace el Runner.
        probe="$CG_DIR/probe-$$"
        # shellcheck disable=SC2016  # comillas simples a proposito: expande el sh interno
        if setpriv --reuid="$RUN_USER" --regid="$RUN_USER" --init-groups \
            sh -c 'mkdir "$1" && echo $$ > "$1/cgroup.procs"' sh "$probe" 2>/dev/null; then
            log "delegacion de cgroups OK ($CG_DIR)"
        else
            log "ERROR: $RUN_USER no puede mover procesos a hojas de $CG_DIR."
            log "       Revisa: slice creada en el host, subtree_control (cpu memory pids) y el bind mount rw."
        fi
        rmdir "$probe" 2>/dev/null || true
    else
        log "AVISO: $CG_DIR no existe; falta el bind mount de goslint.slice."
    fi

    # 4) /work: el Runner crea aqui /work/<uuid> por cada ejecucion. Si es un tmpfs
    #    o un volumen nuevo llega como root:root; se entrega al usuario final sin
    #    depender de las opciones uid/gid del montaje.
    if [ -d "$WORK_DIR" ]; then
        chown "$RUN_USER:$RUN_USER" "$WORK_DIR" 2>/dev/null \
            || log "AVISO: no se pudo dar $WORK_DIR a $RUN_USER"
    else
        log "AVISO: $WORK_DIR no existe"
    fi

    exec setpriv --reuid="$RUN_USER" --regid="$RUN_USER" --init-groups "$@"
fi

# Ya corre sin privilegios (por ejemplo con --user): no hay nada que preparar.
exec "$@"
