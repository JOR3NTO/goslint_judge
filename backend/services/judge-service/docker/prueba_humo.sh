#!/bin/sh
# prueba_humo.sh - Ejecuta el sandbox REAL (mismo comando que el Runner) y comprueba
# aislamiento, seccomp y limites de cgroup.
#
# Uso (desde el host, con el contenedor arriba):
#   docker exec -u ubuntu goslint-judge sh /opt/judge/prueba_humo.sh
#
# Variables (solo para pruebas fuera del contenedor):
#   CG_DIR, WORK_DIR, SECCOMP_PROFILE
#
# Los checks de cgroup (limites, hoja, OOM) solo corren si CG_DIR es cgroup2fs;
# en cualquier otro caso salen como OMITIDO.
# shellcheck disable=SC2015,SC2016  # ok/bad siempre devuelven 0; comillas simples a proposito
set -u

CG="${CG_DIR:-/cg}"
WORK="${WORK_DIR:-/work}"
SECCOMP_PROFILE="${SECCOMP_PROFILE:-/opt/judge/filter.bpf}"
export SECCOMP_PROFILE
# Secreto de prueba: NO debe llegar al codigo evaluado.
export GOSLINT_SECRETO="valor-que-no-debe-verse"

PASS=0; FAIL=0; SKIP=0
TMP="$(mktemp -d)"
LEAVES=""
IS_CG2=0
[ "$(stat -f -c %T "$CG" 2>/dev/null)" = "cgroup2fs" ] && IS_CG2=1

ok()   { echo "  [OK ]    $1"; PASS=$((PASS+1)); }
bad()  { echo "  [FALLO]  $1"; FAIL=$((FAIL+1)); }
skip() { echo "  [OMITIDO] $1"; SKIP=$((SKIP+1)); }
info() { echo "           $1"; }
newid() { cat /proc/sys/kernel/random/uuid; }
get() { printf '%s\n' "$OUT" | sed -n "s/^$1=//p" | head -n1; }

# ---------------------------------------------------------------------------
# Programas de prueba (lo que "ejecutaria" un usuario malicioso o curioso)
# ---------------------------------------------------------------------------
cat > "$TMP/info.py" <<'EOF'
import os, sys
def r(p):
    try:
        return open(p).read()
    except Exception as e:
        return "ERR:" + e.__class__.__name__
me = os.path.basename(sys.argv[0])
pids = [d for d in os.listdir("/proc") if d.isdigit()]
print("PIDS_VISIBLES=%d" % len(pids))
otros = 0
for d in pids:
    c = r("/proc/%s/cmdline" % d).replace("\0", " ")
    if "prog_" in c and me not in c:
        otros += 1
print("OTROS_PROG=%d" % otros)
st = r("/proc/self/status").splitlines()
print("SECCOMP=" + [l for l in st if l.startswith("Seccomp:")][0].split()[1])
print("CAPEFF=" + [l for l in st if l.startswith("CapEff")][0].split()[1])
print("ENV_CLAVES=" + ",".join(sorted(os.environ)))
print("SECRETO_VISIBLE=%d" % int("GOSLINT_SECRETO" in os.environ))
print("RUTAS_CGROUP=%d" % sum(os.path.exists(p) for p in ("/cg", "/tmp/cg-leaf", "/sys/fs/cgroup")))
print("RUN_CONTIENE=" + ",".join(sorted(os.listdir("/run"))))
try:
    os.fstat(3)
    print("FD3_ABIERTO=1")
except OSError:
    print("FD3_ABIERTO=0")
EOF

cat > "$TMP/seccomp.py" <<'EOF'
import ctypes, seccomp
n = seccomp.resolve_syscall(seccomp.Arch.NATIVE, "mount")
print("llamando a mount()...", flush=True)
ctypes.CDLL(None).syscall(n, 0, 0, 0, 0, 0)
print("MOUNT_NO_BLOQUEADO")
EOF

cat > "$TMP/sleep.py" <<'EOF'
import time
time.sleep(6)
print("A_TERMINO")
EOF

cat > "$TMP/fork.py" <<'EOF'
import os, time
n = 0
for _ in range(40):
    try:
        pid = os.fork()
    except OSError:
        break
    if pid == 0:
        time.sleep(3)
        os._exit(0)
    n += 1
print("FORKS_OK=%d" % n)
EOF

cat > "$TMP/mem.py" <<'EOF'
b = bytearray(200 * 1024 * 1024)
for i in range(0, len(b), 4096):
    b[i] = 1
print("NO_MURIO")
EOF

# ---------------------------------------------------------------------------
# Ejecuta un programa igual que el Runner: hoja de cgroup + bwrap + seccomp
# ---------------------------------------------------------------------------
run_sb() {  # $1=id  $2=archivo .py  ->  salida del programa + linea EXIT=<codigo>
    id="$1"; probe="$2"
    W="$WORK/$id"; L="$CG/prog-$id"
    LEAVES="$LEAVES $id"
    mkdir -p "$W" "$L" || { echo "ERROR_PREPARANDO"; echo "EXIT=97"; return; }
    [ -e "$L/cgroup.procs" ] || : > "$L/cgroup.procs"
    if [ "$IS_CG2" = 1 ]; then
        { echo 52428800 > "$L/memory.max" && echo 5 > "$L/pids.max"; } 2>/dev/null \
            || echo "ERROR_LIMITES (falta memory/pids en subtree_control?)"
    fi
    cp "$probe" "$W/prog_$id.py"
    timeout 30 sh -c 'exec bwrap "$@" 3<"$SECCOMP_PROFILE"' sh \
        --die-with-parent --new-session --clearenv --setenv PATH /usr/bin:/bin \
        --unshare-user --unshare-ipc --unshare-pid --unshare-net --unshare-uts \
        --ro-bind /usr /usr --symlink usr/bin /bin --symlink usr/lib /lib --symlink usr/lib64 /lib64 \
        --proc /proc --dev /dev --size 52428800 --tmpfs /tmp \
        --ro-bind "$W" /work --chdir /work \
        --bind "$L/cgroup.procs" /run/cgp \
        --seccomp 3 \
        -- /bin/sh -c 'echo $$ > /run/cgp; exec "$@"' -- python3 "prog_$id.py" </dev/null 2>&1
    echo "EXIT=$?"
}

cleanup() {
    for id in $LEAVES; do
        rmdir "$CG/prog-$id" 2>/dev/null || rm -rf "$CG/prog-$id" 2>/dev/null
        rm -rf "${WORK:?}/$id" 2>/dev/null
    done
    rm -rf "$TMP"
}
trap cleanup EXIT

echo "Sandbox: cgroup=$CG (cgroup2: $([ $IS_CG2 = 1 ] && echo si || echo NO)) | work=$WORK | seccomp=$SECCOMP_PROFILE"
echo "Arquitectura: $(uname -m) | kernel: $(uname -r) | usuario: $(id -un)"

# ---------------------------------------------------------------------------
echo; echo "1) Aislamiento visto desde DENTRO del sandbox"
ID1="$(newid)"
OUT="$(run_sb "$ID1" "$TMP/info.py")"
if [ "$(get EXIT)" != "0" ]; then
    bad "bwrap no arranco (EXIT=$(get EXIT)). Salida:"
    printf '%s\n' "$OUT" | sed 's/^/           | /'
    echo "           Pistas: error con /proc -> revisa systempaths=unconfined; con mount/pivot_root -> seccomp/apparmor;"
    echo "                   'Bad file descriptor' -> el fd 3 del filtro no se abrio."
    exit 2
fi
ok "bwrap arranca con --proc /proc, seccomp y hoja de cgroup"

n="$(get PIDS_VISIBLES)"
if [ "$n" -le 3 ]; then ok "/proc corresponde a su PID namespace ($n procesos visibles)"
else bad "/proc expone $n procesos: es el del contenedor, no el del sandbox"; fi

[ "$(get SECCOMP)" = "2" ] && ok "filtro seccomp activo (Seccomp: 2)" || bad "seccomp NO activo (Seccomp: $(get SECCOMP))"
[ "$(get CAPEFF)" = "0000000000000000" ] && ok "sin capabilities efectivas" || bad "capabilities efectivas: $(get CAPEFF)"
[ "$(get SECRETO_VISIBLE)" = "0" ] && ok "el entorno del servicio no se hereda" || bad "el codigo VE variables de entorno del servicio"
extra="$(get ENV_CLAVES | tr ',' '\n' | grep -v -x -E 'PATH|PWD|LC_CTYPE|LANG|LC_ALL' | tr '\n' ' ')"
[ -z "$extra" ] && ok "entorno minimo: $(get ENV_CLAVES)" || bad "variables inesperadas en el entorno: $extra"
[ "$(get RUTAS_CGROUP)" = "0" ] && ok "no ve /cg, /tmp/cg-leaf ni /sys/fs/cgroup" || bad "ve $(get RUTAS_CGROUP) ruta(s) de cgroup"
[ "$(get RUN_CONTIENE)" = "cgp" ] && ok "unico archivo de control expuesto: /run/cgp" || bad "/run contiene: $(get RUN_CONTIENE)"
[ "$(get FD3_ABIERTO)" = "0" ] && ok "el fd 3 del filtro no se filtra al programa" || bad "el fd 3 sigue abierto en el programa"

if [ "$IS_CG2" = 1 ]; then
    cpu="$(sed -n 's/^usage_usec //p' "$CG/prog-$ID1/cpu.stat")"
    info "CPU contabilizada en la hoja para un programa trivial: ${cpu} us (deberia ser de pocos ms; si es de cientos de ms, se cuela ruido de preparacion)"
fi

# ---------------------------------------------------------------------------
echo; echo "2) seccomp bloquea mount() al codigo del usuario"
ID2="$(newid)"; OUT="$(run_sb "$ID2" "$TMP/seccomp.py")"
if [ "$(get EXIT)" = "159" ]; then ok "mount() mata el proceso con SIGSYS (EXIT=159)"
else bad "mount() no fue bloqueado (EXIT=$(get EXIT))"; fi

# ---------------------------------------------------------------------------
echo; echo "3) Dos ejecuciones simultaneas no se ven entre si"
IDA="$(newid)"; IDB="$(newid)"
( run_sb "$IDA" "$TMP/sleep.py" > "$TMP/A.out" ) &
APID=$!
sleep 2
if [ "$IS_CG2" = 1 ]; then
    procs="$(cat "$CG/prog-$IDA/cgroup.procs" 2>/dev/null)"
    cnt="$(printf '%s\n' "$procs" | grep -c .)"
    nombres=""
    for p in $procs; do nombres="$nombres [$({ tr '\0' ' ' < "/proc/$p/cmdline"; } 2>/dev/null)]"; done
    if [ "$cnt" -eq 1 ] && printf '%s' "$nombres" | grep -q python3; then
        ok "la hoja contiene SOLO el programa evaluado:$nombres"
    else
        bad "la hoja contiene $cnt proceso(s):$nombres (se esperaba solo python3)"
    fi
else
    skip "contenido de la hoja de cgroup (no es cgroup2)"
fi
OUT="$(run_sb "$IDB" "$TMP/info.py")"
[ "$(get OTROS_PROG)" = "0" ] && ok "la ejecucion B no ve procesos de la ejecucion A" \
    || bad "la ejecucion B ve $(get OTROS_PROG) proceso(s) de otra ejecucion (fuga por /proc)"
wait "$APID"
grep -q "A_TERMINO" "$TMP/A.out" && ok "la ejecucion A no se vio afectada por B" || bad "la ejecucion A no termino bien"

# ---------------------------------------------------------------------------
echo; echo "4) Limites de cgroup (fork bomb y memoria)"
IDF="$(newid)"; OUT="$(run_sb "$IDF" "$TMP/fork.py")"
if [ "$IS_CG2" = 1 ]; then
    f="$(get FORKS_OK)"
    max_ev="$(sed -n 's/^max //p' "$CG/prog-$IDF/pids.events" 2>/dev/null)"
    if [ -n "$f" ] && [ "$f" -le 4 ] && [ "${max_ev:-0}" -gt 0 ]; then
        ok "fork bomb limitada por pids.max=5 ($f forks; pids.events max=$max_ev)"
    else
        bad "fork bomb NO limitada (forks=$f, pids.events max=${max_ev:-?})"
    fi
else
    skip "fork bomb (no es cgroup2)"
fi

IDM="$(newid)"; OUT="$(run_sb "$IDM" "$TMP/mem.py")"
if [ "$IS_CG2" = 1 ]; then
    oom="$(sed -n 's/^oom_kill //p' "$CG/prog-$IDM/memory.events" 2>/dev/null)"
    peak="$(cat "$CG/prog-$IDM/memory.peak" 2>/dev/null)"
    mx="$(cat "$CG/prog-$IDM/memory.max" 2>/dev/null)"
    if [ "${oom:-0}" -ge 1 ]; then ok "OOM-kill dentro de la hoja (oom_kill=$oom, EXIT=$(get EXIT))"
    else bad "el programa paso el limite de memoria sin OOM-kill (EXIT=$(get EXIT))"; fi
    info "memory.peak=$peak vs memory.max=$mx -> el pico no supera el limite: detecta MLE con oom_kill de memory.events, no con peak > limite"
else
    skip "limite de memoria (no es cgroup2)"
fi

# ---------------------------------------------------------------------------
echo; echo "5) Limpieza"
sleep 1
fugas=0
# Se busca el UUID de CADA ejecucion de esta prueba (no un texto generico): asi no se
# confunde con procesos ajenos, ni con la propia busqueda. Se compara con case, sin grep.
for d in /proc/[0-9]*; do
    c="$({ tr '\0' ' ' < "$d/cmdline"; } 2>/dev/null)"
    for id in $LEAVES; do
        case "$c" in *"$id"*) fugas=$((fugas+1)); break ;; esac
    done
done
[ "$fugas" -eq 0 ] && ok "no quedan procesos de las ejecuciones" || bad "quedan $fugas proceso(s) de ejecuciones anteriores"
malas=0
for id in $LEAVES; do
    if [ "$IS_CG2" = 1 ]; then rmdir "$CG/prog-$id" 2>/dev/null || malas=$((malas+1))
    else rm -rf "$CG/prog-$id"; fi
done
LEAVES=""
if [ "$IS_CG2" = 1 ]; then
    [ "$malas" -eq 0 ] && ok "todas las hojas se borran con rmdir" || bad "$malas hoja(s) no se pudieron borrar (aun tienen procesos)"
else
    skip "borrado de hojas (no es cgroup2)"
fi

echo; echo "Resultado: $PASS OK | $FAIL FALLO | $SKIP OMITIDO"
[ "$FAIL" -eq 0 ]
