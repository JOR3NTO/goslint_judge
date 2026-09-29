#!/usr/bin/env python3
"""
gen_seccomp.py - Genera el filtro seccomp (lista negra) para el codigo evaluado.

Uso:
    python3 gen_seccomp.py                      # escribe /opt/judge/filter.bpf
    python3 gen_seccomp.py -o ./filter.bpf      # otra ruta de salida
    python3 gen_seccomp.py --pfc                # ademas, vuelca el filtro legible (.pfc) para auditarlo
    python3 gen_seccomp.py --verify             # genera y prueba cada regla cargando el filtro en procesos hijo

El .bpf resultante es el que bwrap carga con  --seccomp <fd>.
Genéralo DENTRO de la imagen (build) o del contenedor: el binario depende de la
arquitectura y de la version de libseccomp, no lo copies entre maquinas distintas.

Politica: accion por defecto ALLOW; se niega lo listado abajo.
  - KILL_PROCESS : syscalls que ningun programa legitimo del juez necesita.
                   El proceso muere con SIGSYS (codigo de salida 159 en shell).
  - ERRNO        : sondeos que un runtime podria intentar; falla con error normal.
Los argumentos de arquitectura no nativa se matan por defecto (libseccomp).
"""
import argparse
import errno
import os
import sys

import seccomp

# ---------------------------------------------------------------------------
# 1) KILL_PROCESS: nunca deberian ejecutarse en una solucion de concurso
# ---------------------------------------------------------------------------
KILL = {
    # Montajes, namespaces y cambio de raiz (bwrap ya termino de usarlos)
    "montajes/namespaces": [
        "mount", "umount", "umount2", "pivot_root", "chroot",
        "unshare", "setns",
        "move_mount", "open_tree", "fsopen", "fsconfig", "fsmount", "fspick",
        "mount_setattr",
    ],
    # Espiar o manipular otros procesos
    "procesos ajenos": [
        "ptrace", "process_vm_readv", "process_vm_writev", "process_madvise",
        "kcmp", "pidfd_getfd",
    ],
    # Superficie de ataque del kernel con historial alto de vulnerabilidades
    "superficie de kernel": [
        "bpf", "io_uring_setup", "io_uring_enter", "io_uring_register",
        "userfaultfd", "perf_event_open",
        "keyctl", "add_key", "request_key",
    ],
    # Kernel, arranque y host
    "kernel/host": [
        "init_module", "finit_module", "delete_module", "create_module",
        "get_kernel_syms", "query_module",
        "kexec_load", "kexec_file_load", "reboot",
        "swapon", "swapoff", "acct", "quotactl", "quotactl_fd",
        "nfsservctl", "lookup_dcookie", "uselib", "_sysctl",
    ],
    # Escape por file handles (tipo "Shocker")
    "file handles": ["open_by_handle_at", "name_to_handle_at"],
    # Especificas de x86
    "x86": ["iopl", "ioperm", "modify_ldt", "vm86", "vm86old"],
    # Reloj y nombre del host
    "reloj/hostname": [
        "settimeofday", "stime", "clock_settime", "clock_adjtime", "adjtimex",
        "sethostname", "setdomainname",
    ],
}

# ---------------------------------------------------------------------------
# 2) clone() con flags de namespace -> KILL
#    unshare ya esta bloqueado, pero clone tambien puede crear namespaces.
#    (En x86_64 y aarch64 los flags van en el argumento 0. CLONE_NEWTIME solo
#    existe via unshare/clone3, ambos cubiertos.)
# ---------------------------------------------------------------------------
CLONE_NS_FLAGS = {
    "CLONE_NEWNS":     0x00020000,
    "CLONE_NEWCGROUP": 0x02000000,
    "CLONE_NEWUTS":    0x04000000,
    "CLONE_NEWIPC":    0x08000000,
    "CLONE_NEWUSER":   0x10000000,
    "CLONE_NEWPID":    0x20000000,
    "CLONE_NEWNET":    0x40000000,
}

# ---------------------------------------------------------------------------
# 3) ERRNO: sondeos tolerables
# ---------------------------------------------------------------------------
# clone3 recibe sus flags en una estructura en memoria: seccomp no puede
# inspeccionarla. Se responde ENOSYS para que glibc (>= 2.34) use clone().
CLONE3_ERRNO = errno.ENOSYS

# Familias de sockets con historial de vulnerabilidades locales. Quedan
# permitidas AF_UNIX, AF_INET y AF_INET6 (sin red igualmente por --unshare-net).
# Compromiso conocido: bloquear AF_NETLINK impide getifaddrs()/NetworkInterface.
BLOCKED_SOCKET_FAMILIES = {
    "AF_AX25": 3, "AF_IPX": 4, "AF_APPLETALK": 5, "AF_NETROM": 6,
    "AF_ATMPVC": 8, "AF_X25": 9, "AF_ROSE": 11, "AF_DECnet": 12,
    "AF_KEY": 15, "AF_NETLINK": 16, "AF_PACKET": 17, "AF_ATMSVC": 20,
    "AF_RDS": 21, "AF_IRDA": 23, "AF_PPPOX": 24, "AF_LLC": 26,
    "AF_CAN": 29, "AF_TIPC": 30, "AF_BLUETOOTH": 31, "AF_ALG": 38,
    "AF_NFC": 39, "AF_VSOCK": 40, "AF_KCM": 41, "AF_QIPCRTR": 42,
    "AF_SMC": 43, "AF_XDP": 44,
}

TIOCSTI = 0x5412      # inyectar teclas en la terminal
TIOCLINUX = 0x541C
ADDR_NO_RANDOMIZE = 0x0040000   # personality(): desactivar ASLR

SYSLOG_ERRNO = errno.EPERM      # lectura del log del kernel (dmesg)


def build_filter():
    """Devuelve (filtro, omitidas) donde omitidas son syscalls que no existen en esta arquitectura."""
    f = seccomp.SyscallFilter(defaction=seccomp.ALLOW)
    omitidas = []

    def add(action, name, *args):
        # En arquitecturas donde la syscall no existe (p. ej. iopl en ARM) libseccomp
        # devuelve un numero negativo y add_rule la acepta en silencio sin hacer nada:
        # se detecta aqui para que el conteo y la lista de omitidas sean reales.
        if seccomp.resolve_syscall(seccomp.Arch.NATIVE, name) < 0:
            omitidas.append(name)
            return
        try:
            f.add_rule(action, name, *args)
        except (RuntimeError, OSError):
            omitidas.append(name)

    for nombres in KILL.values():
        for nombre in nombres:
            add(seccomp.KILL_PROCESS, nombre)

    for flag in CLONE_NS_FLAGS.values():
        # Una regla por flag (varias reglas de la misma syscall se combinan con OR)
        add(seccomp.KILL_PROCESS, "clone", seccomp.Arg(0, seccomp.MASKED_EQ, flag, flag))

    add(seccomp.ERRNO(CLONE3_ERRNO), "clone3")

    for familia in BLOCKED_SOCKET_FAMILIES.values():
        add(seccomp.ERRNO(errno.EAFNOSUPPORT), "socket", seccomp.Arg(0, seccomp.EQ, familia))
        add(seccomp.ERRNO(errno.EAFNOSUPPORT), "socketpair", seccomp.Arg(0, seccomp.EQ, familia))

    add(seccomp.ERRNO(errno.EPERM), "ioctl", seccomp.Arg(1, seccomp.EQ, TIOCSTI))
    add(seccomp.ERRNO(errno.EPERM), "ioctl", seccomp.Arg(1, seccomp.EQ, TIOCLINUX))
    add(seccomp.ERRNO(errno.EPERM), "personality",
        seccomp.Arg(0, seccomp.MASKED_EQ, ADDR_NO_RANDOMIZE, ADDR_NO_RANDOMIZE))
    add(seccomp.ERRNO(SYSLOG_ERRNO), "syslog")

    return f, omitidas


# ---------------------------------------------------------------------------
# Verificacion: carga el filtro en procesos hijo y comprueba el resultado
# ---------------------------------------------------------------------------
def _verify(f):
    import ctypes
    import signal
    import socket
    import subprocess
    import threading

    libc = ctypes.CDLL(None, use_errno=True)
    libc.syscall.restype = ctypes.c_long

    def nr(nombre):
        return seccomp.resolve_syscall(seccomp.Arch.NATIVE, nombre)

    def sc(nombre, *args):
        vals = [ctypes.c_long(a) for a in args]
        return libc.syscall(ctypes.c_long(nr(nombre)), *vals)

    # Cada caso devuelve el codigo de salida del hijo (0 = comportamiento esperado)
    def c_syscall_mata(nombre, *args):
        def run():
            sc(nombre, *args)
            return 90  # si llega aqui, NO fue bloqueada
        return run

    def c_clone_ns(flag):
        def run():
            r = sc("clone", flag | signal.SIGCHLD, 0, 0, 0, 0)
            if r == 0:
                os._exit(91)    # hijo creado: NO fue bloqueada
            return 90
        return run

    def c_errno(esperado, nombre, *args):
        def run():
            r = sc(nombre, *args)
            e = ctypes.get_errno()
            return 0 if (r == -1 and e == esperado) else 92
        return run

    def c_socket(fam, debe_fallar):
        def run():
            try:
                s = socket.socket(fam, socket.SOCK_STREAM if fam != 1 else socket.SOCK_STREAM)
                s.close()
                return 92 if debe_fallar else 0
            except OSError as e:
                if debe_fallar:
                    return 0 if e.errno == errno.EAFNOSUPPORT else 92
                return 93
        return run

    def c_permitido():
        def run():
            # Lo que un runtime legitimo si necesita
            hilos = []
            for _ in range(3):
                t = threading.Thread(target=lambda: None)
                t.start()
                hilos.append(t)
            for t in hilos:
                t.join()
            pid = os.fork()
            if pid == 0:
                os._exit(0)
            os.waitpid(pid, 0)
            r = subprocess.run(["/bin/true"])
            with open("/proc/self/status") as fh:
                estado = fh.read()
            if r.returncode != 0 or "Seccomp:\t2" not in estado:
                return 94
            return 0
        return run

    NEWNET, NEWUSER = CLONE_NS_FLAGS["CLONE_NEWNET"], CLONE_NS_FLAGS["CLONE_NEWUSER"]
    casos = [
        # (descripcion, funcion, resultado esperado: "SIGSYS" o 0)
        ("mount            -> KILL",   c_syscall_mata("mount", 0, 0, 0, 0, 0), "SIGSYS"),
        ("ptrace           -> KILL",   c_syscall_mata("ptrace", 0, 0, 0, 0), "SIGSYS"),
        ("unshare(NEWUSER) -> KILL",   c_syscall_mata("unshare", NEWUSER), "SIGSYS"),
        ("setns            -> KILL",   c_syscall_mata("setns", -1, 0), "SIGSYS"),
        ("bpf              -> KILL",   c_syscall_mata("bpf", 0, 0, 0), "SIGSYS"),
        ("io_uring_setup   -> KILL",   c_syscall_mata("io_uring_setup", 1, 0), "SIGSYS"),
        ("userfaultfd      -> KILL",   c_syscall_mata("userfaultfd", 0), "SIGSYS"),
        ("keyctl           -> KILL",   c_syscall_mata("keyctl", 0, 0, 0, 0, 0), "SIGSYS"),
        ("open_by_handle_at-> KILL",   c_syscall_mata("open_by_handle_at", -1, 0, 0), "SIGSYS"),
        ("clone(NEWNET)    -> KILL",   c_clone_ns(NEWNET), "SIGSYS"),
        ("clone(NEWUSER)   -> KILL",   c_clone_ns(NEWUSER), "SIGSYS"),
        ("clone3           -> ENOSYS", c_errno(errno.ENOSYS, "clone3", 0, 0), 0),
        ("ioctl TIOCSTI    -> EPERM",  c_errno(errno.EPERM, "ioctl", 0, TIOCSTI, 0), 0),
        ("personality NO_RANDOMIZE -> EPERM", c_errno(errno.EPERM, "personality", ADDR_NO_RANDOMIZE), 0),
        ("socket AF_NETLINK-> EAFNOSUPPORT", c_socket(16, True), 0),
        ("socket AF_PACKET -> EAFNOSUPPORT", c_socket(17, True), 0),
        ("socket AF_UNIX   permitido", c_socket(1, False), 0),
        ("socket AF_INET   permitido", c_socket(2, False), 0),
        ("hilos, fork, subprocess, /proc/self/status", c_permitido(), 0),
    ]

    fallos = 0
    for desc, fn, esperado in casos:
        pid = os.fork()
        if pid == 0:
            try:
                f.load()
                os._exit(fn())
            except BaseException:
                os._exit(99)
        _, status = os.waitpid(pid, 0)
        if os.WIFSIGNALED(status):
            obtenido = "SIGSYS" if os.WTERMSIG(status) == signal.SIGSYS else "signal %d" % os.WTERMSIG(status)
        else:
            obtenido = os.WEXITSTATUS(status)
        ok = obtenido == esperado
        fallos += 0 if ok else 1
        print("  [%s] %-42s (obtenido: %s)" % ("OK " if ok else "FALLO", desc, obtenido))
    return fallos


def main():
    ap = argparse.ArgumentParser(description="Genera el filtro seccomp (lista negra) para bwrap --seccomp")
    ap.add_argument("-o", "--output", default="/opt/judge/filter.bpf")
    ap.add_argument("--pfc", action="store_true", help="tambien escribe <output>.pfc legible")
    ap.add_argument("--verify", action="store_true", help="prueba cada regla en procesos hijo")
    args = ap.parse_args()

    f, omitidas = build_filter()

    os.makedirs(os.path.dirname(os.path.abspath(args.output)), exist_ok=True)
    with open(args.output, "wb") as fh:
        f.export_bpf(fh)
    total = sum(len(v) for v in KILL.values())
    print("Filtro escrito en %s (%d syscalls KILL, %d flags de clone, %d familias de socket)"
          % (args.output, total - len(omitidas), len(CLONE_NS_FLAGS), len(BLOCKED_SOCKET_FAMILIES)))
    if omitidas:
        print("No existen en esta arquitectura (omitidas): " + ", ".join(sorted(set(omitidas))))

    if args.pfc:
        with open(args.output + ".pfc", "w") as fh:
            f.export_pfc(fh)
        print("Version legible: %s.pfc" % args.output)

    if args.verify:
        print("Verificando reglas (cada caso en un proceso hijo con el filtro cargado):")
        fallos = _verify(f)
        if fallos:
            print("%d caso(s) fallaron." % fallos)
            sys.exit(1)
        print("Todos los casos pasaron.")


if __name__ == "__main__":
    main()
