# Bubblewrap en el sandbox — `judge-service`

> Cómo se encierra el código de un estudiante para que no vea el sistema, la red ni las demás evaluaciones.
> Documento vivo: actualizar cuando cambie el comando de bwrap o el filtro de syscalls.

---

## Tabla de contenido

1. [Qué hace y qué no hace](#1-qué-hace-y-qué-no-hace)
2. [Dónde encaja cada pieza](#2-dónde-encaja-cada-pieza)
3. [El comando](#3-el-comando)
4. [Qué ve el programa](#4-qué-ve-el-programa)
5. [El caso de `/proc`](#5-el-caso-de-proc)
6. [seccomp: el filtro de syscalls](#6-seccomp-el-filtro-de-syscalls)
7. [Cómo se une con el cgroup](#7-cómo-se-une-con-el-cgroup)
8. [Ciclo de vida y limpieza](#8-ciclo-de-vida-y-limpieza)
9. [Códigos de salida de bwrap](#9-códigos-de-salida-de-bwrap)
10. [Sin `--privileged` ni capabilities](#10-sin---privileged-ni-capabilities)
11. [Mapa de archivos](#11-mapa-de-archivos)
12. [Limitaciones conocidas](#12-limitaciones-conocidas)
13. [Estado de la validación](#13-estado-de-la-validación)

---

## 1. Qué hace y qué no hace

Bubblewrap (`bwrap`) arma, para un solo programa, una vista reducida del sistema: su propio árbol de procesos, su propio sistema de archivos, sin red y sin las variables de entorno del servicio. Eso es todo lo que hace. No limita CPU ni memoria, no mide consumo y no decide qué llamadas al sistema puede usar el programa.

Cada una de esas tareas pertenece a un mecanismo distinto, y ninguno sustituye a otro:

| Mecanismo | Pregunta que responde | Cómo se aplica |
|---|---|---|
| Bubblewrap | ¿Qué puede ver y tocar el programa? | Namespaces y montajes |
| seccomp | ¿Qué puede pedirle al kernel? | Filtro BPF que bwrap carga con `--seccomp` |
| cgroup v2 | ¿Cuánto puede consumir y cuánto consumió? | Una hoja de cgroup por ejecución — ver [CGROUPS.md](./CGROUPS.md) |

Un programa que cabe en el sandbox pero no tiene cgroup puede agotar la memoria del servidor. Uno con cgroup pero sin bwrap puede leer los archivos de otras ejecuciones. Los tres van juntos.

---

## 2. Dónde encaja cada pieza

```
host (Ubuntu, Docker Engine nativo)
└── contenedor persistente del sandbox          (usuario sin privilegios, sin capabilities)
    └── JVM: judge-service                      (un TestCaseRunner por caso de prueba)
        └── sh externo          abre el filtro seccomp como fd 3 y hace exec
            └── bwrap           crea namespaces y monta el sistema de archivos
                ├── init de bwrap (PID 1 del namespace)         fuera de la hoja de cgroup
                └── sh -c 'echo $$ > /run/cgp; exec "$@"'       se enrola en la hoja y hace exec
                    └── python3 solution.py                     único proceso dentro de la hoja
```

El contenedor es uno solo y persistente. No se crea un contenedor por evaluación ni se usa Docker dentro de Docker: cada ejecución es un `bwrap` nuevo dentro del mismo contenedor.

---

## 3. El comando

Lo construye [BwrapCommandFactory](../src/main/java/co/uceva/judge/infrastructure/sandbox/command/BwrapCommandFactory.java) y lo lanza `TestCaseRunner` con `ProcessBuilder`:

```sh
sh -c 'exec bwrap "$@" 3<"$SECCOMP_PROFILE"' sh \
  --die-with-parent --new-session --clearenv --setenv PATH /usr/bin:/bin \
  --unshare-user --unshare-ipc --unshare-pid --unshare-net --unshare-uts \
  --ro-bind /usr /usr --symlink usr/bin /bin --symlink usr/lib /lib --symlink usr/lib64 /lib64 \
  --proc /proc --dev /dev \
  --ro-bind /work/<uuid> /solution \
  --size 52428800 --tmpfs /work \
  --tmpfs /tmp \
  --chdir /work \
  --bind /cg/prog-<uuid>/cgroup.procs /run/cgp \
  --seccomp 3 \
  -- /bin/sh -c 'echo $$ > /run/cgp; exec "$@"' -- python3 /solution/solution.py
```

| Flag | Qué hace | Por qué está |
|---|---|---|
| `--die-with-parent` | Si el proceso padre muere, bwrap y todo el namespace mueren | Sin él, un servicio caído deja programas huérfanos corriendo |
| `--new-session` | Nueva sesión de terminal | Cierra la inyección de teclas con `ioctl(TIOCSTI)` |
| `--clearenv --setenv PATH ...` | Entorno vacío salvo `PATH` | El código no debe heredar credenciales del servicio |
| `--unshare-user` | Nuevo user namespace | Permite a bwrap montar sin ser root; dentro, el programa no tiene capabilities |
| `--unshare-pid` | Nuevo PID namespace | El programa solo ve sus propios procesos |
| `--unshare-net` | Nuevo network namespace | Sin red, solo loopback |
| `--unshare-ipc`, `--unshare-uts` | IPC y hostname propios | Sin memoria compartida ni colas con otras ejecuciones |
| `--ro-bind /usr /usr` y los `--symlink` | Intérpretes y librerías, en solo lectura | Es lo único del sistema que el programa necesita |
| `--proc /proc` | procfs nuevo, ligado al PID namespace | Ver sección 5 |
| `--dev /dev` | `/dev` mínimo | `null`, `zero`, `urandom` y poco más |
| `--ro-bind /work/<uuid> /solution` | La carpeta de **esta** ejecución, en solo lectura | El código no puede modificar su propio fuente |
| `--size ... --tmpfs /work` | Directorio de trabajo escribible, con tope | Un tmpfs sin `--size` se dimensiona a la mitad de la RAM; `--size` debe ir **antes** del `--tmpfs` |
| `--tmpfs /tmp` | Temporales del programa | Cuenta contra el `memory.max` de la hoja, igual que `/work` |
| `--chdir /work` | Directorio de trabajo inicial | El programa escribe ahí, no donde está su fuente |
| `--bind .../cgroup.procs /run/cgp` | Un único archivo de control | Ver sección 7 |
| `--seccomp 3` | Carga el filtro BPF del descriptor 3 | Ver sección 6 |

> El espacio de `/work` y `/tmp` cuenta contra el `memory.max` de la hoja, porque un tmpfs vive en memoria ([CGROUPS.md](./CGROUPS.md), sección 7).

---

## 4. Qué ve el programa

```
/                 tmpfs nuevo y vacío
├── usr           solo lectura (el /usr del contenedor)
├── bin  -> usr/bin        lib -> usr/lib        lib64 -> usr/lib64
├── proc          procfs nuevo del PID namespace
├── dev           dispositivos mínimos
├── solution      solo lectura: el fuente de ESTA ejecución
├── work          tmpfs escribible y limitado; directorio de trabajo
├── tmp           tmpfs escribible
└── run/cgp       un único archivo: el cgroup.procs de ESTA ejecución
```

No existen dentro del sandbox: el resto del contenedor, `/etc`, `/sys`, `/cg`, `/opt/judge`, los directorios de otras ejecuciones ni ninguna ruta de cgroup.

Consecuencia práctica: la solución tiene que estar dentro de `/work/<uuid>` **antes** de lanzar bwrap. `SandboxWorkspace` la copia allí; si no lo hiciera, el intérprete respondería que no encuentra el archivo.

**Lenguajes.** El diseño se probó con Python, que es también el único que `RunnerSandboxExecutor` acepta hoy. Al no haber `/etc`, otros lenguajes pueden necesitar montajes extra (por ejemplo, Java busca su configuración en `/etc/java-17-openjdk`). Java, C y C++ están **sin probar** en este sandbox, y su compilación es otra historia de usuario.

---

## 5. El caso de `/proc`

**El problema.** Usar `--bind /proc /proc` monta el `/proc` del contenedor. Aunque el programa esté en su propio PID namespace, vería los procesos de todo el contenedor: los de otras ejecuciones y los del servicio, y podría leer su `cmdline`, `status` y `cgroup`. Como el `cmdline` de otra ejecución incluye la ruta de su solución, sería además un camino hacia el código ajeno.

**Por qué no basta `--proc /proc`.** El kernel no deja montar un procfs nuevo desde un user namespace si el `/proc` existente tiene partes tapadas, y Docker tapa varias rutas de `/proc` por defecto. **No se arregla con capabilities.**

**Qué lo resuelve.** La opción de Docker `--security-opt systempaths=unconfined` quita esas máscaras sin `--privileged` y sin capabilities. Con ella, `--proc /proc` funciona y cada ejecución recibe un procfs limpio.

**Qué se comprobó en el host:**

- el programa ve 2 procesos (el init de bwrap y él mismo);
- una ejecución B no ve ningún proceso de una ejecución A que corre al mismo tiempo;
- la hoja de cgroup de A contiene solo su programa.

**Coste.** Quitar las máscaras es una relajación real del contenedor, aunque pesa poco: el servicio corre como usuario sin privilegios y sin capabilities. Queda documentada como decisión consciente.

---

## 6. seccomp: el filtro de syscalls

**Por qué hace falta.** Para que bwrap monte y cambie de raíz, el contenedor corre con `seccomp=unconfined`. Eso desactiva también el filtro por defecto de Docker, y el código del usuario quedaría sin ninguno.

**Cómo se resuelve.** bwrap acepta `--seccomp FD` y aplica ese filtro justo antes del `exec` del programa, cuando ya terminó de usar `mount` y `pivot_root`. bwrap sí puede montar; el código evaluado no.

**Cómo llega el filtro.** `ProcessBuilder` no puede pasar descriptores arbitrarios, así que un `sh` externo abre el archivo como fd 3 y hace `exec` a bwrap:

```sh
sh -c 'exec bwrap "$@" 3<"$SECCOMP_PROFILE"' sh <argumentos de bwrap>
```

La ruta del filtro (`/opt/judge/filter.bpf`) viaja como variable de entorno `SECCOMP_PROFILE` del proceso externo, no del sandbox: bwrap lee el filtro y cierra el descriptor, así que dentro del sandbox el fd 3 no existe.

**Qué bloquea** (lista negra del generador del filtro):

- Con `KILL_PROCESS` (el programa muere con SIGSYS, código de salida 159): montajes y cambios de namespace, `ptrace` y acceso a memoria ajena, `bpf`, `io_uring_*`, `userfaultfd`, `perf_event_open`, `keyctl`, módulos y arranque del kernel, `open_by_handle_at`, reloj y hostname, y `clone` con flags de namespace.
- Con `ERRNO` (falla con error normal): `clone3` (ENOSYS, para que glibc use `clone`), 26 familias de socket con historial de vulnerabilidades (entre ellas `AF_NETLINK`, `AF_PACKET`, `AF_ALG`, `AF_VSOCK`), `ioctl(TIOCSTI)`, `personality` para apagar ASLR y `syslog`.

**Cómo se genera y se verifica.** El filtro depende de la arquitectura y de la versión de libseccomp, por eso se genera al construir la imagen del sandbox y se verifica con su propio script, que carga el filtro en procesos hijo y comprueba 19 comportamientos. Ese generador vive en el paquete del sandbox, todavía fuera de este repositorio ([REQUIREMENTS.md](./REQUIREMENTS.md), anexo).

---

## 7. Cómo se une con el cgroup

El programa se enrola en su hoja de cgroup desde dentro del sandbox: el `sh` escribe su propio PID en `/run/cgp` y hace `exec` al programa. Tres decisiones explican el diseño:

- **Solo `cgroup.procs`, no la hoja entera.** Si el sandbox viera `memory.max` o `pids.max`, el código podría reescribirlos (`echo max > pids.max`). Con un solo archivo expuesto no puede tocar ningún límite ni consultar otros cgroups.
- **El enrolamiento ocurre dentro del sandbox.** El kernel interpreta el PID según el namespace de quien escribe, así que el `$$` del sandbox (que vale 2) es válido.
- **bwrap se queda fuera de la hoja.** Su init y el propio bwrap no cuentan en `cpu.stat`, ni en `memory.peak`, ni en el presupuesto de `pids.max`. La hoja mide únicamente el programa evaluado.

Los límites en sí se explican en [CGROUPS.md](./CGROUPS.md).

---

## 8. Ciclo de vida y limpieza

- Cuando el proceso principal termina, bwrap devuelve el control casi de inmediato y los hijos que hubiera dejado mueren con el PID namespace.
- Matar el proceso `bwrap` con SIGKILL elimina todo lo que contiene, incluso hijos que ignoran señales, gracias a `--die-with-parent`. Por eso el watchdog usa `destroyForcibly()` y no `destroy()`.
- En el host se comprobó que tras una fork bomb y un OOM-kill no queda ningún proceso y todas las hojas se borran.

---

## 9. Códigos de salida de bwrap

bwrap propaga el estado del programa: un código normal se devuelve tal cual, y un programa muerto por una señal sale como 128 más el número de la señal.

| Salida | Significado habitual |
|---|---|
| 0 | El programa terminó bien |
| 1..126 | Error del programa (o de bwrap al montar, si aparece un mensaje `bwrap:` en stderr) |
| 127 | El comando no existe dentro del sandbox |
| 137 | SIGKILL: OOM-kill, watchdog o límite de salida |
| 159 | SIGSYS: seccomp bloqueó una syscall |

137 por sí solo no distingue entre memoria y tiempo: `TestCaseRunner` lo decide con `oom_kill` de `memory.events` y con la bandera de TLE del watchdog.

---

## 10. Sin `--privileged` ni capabilities

El contenedor no usa `--privileged` ni `--cap-add`. Lo que hace falta son tres relajaciones de seguridad del runtime: `seccomp=unconfined`, `apparmor=unconfined` y `systempaths=unconfined`. Las capabilities efectivas del usuario del contenedor son cero, y dentro del sandbox también. Los flags concretos están en [REQUIREMENTS.md](./REQUIREMENTS.md), sección 5.

---

## 11. Mapa de archivos

| Archivo | Rol |
|---------|-----|
| [infrastructure/sandbox/command/BwrapCommandFactory.java](../src/main/java/co/uceva/judge/infrastructure/sandbox/command/BwrapCommandFactory.java) | Construye el comando completo de bwrap |
| [infrastructure/sandbox/workspace/SandboxWorkspace.java](../src/main/java/co/uceva/judge/infrastructure/sandbox/workspace/SandboxWorkspace.java) | Hoja de cgroup, `/work/<uuid>` y ruta del filtro seccomp |
| [infrastructure/sandbox/execution/TestCaseRunner.java](../src/main/java/co/uceva/judge/infrastructure/sandbox/execution/TestCaseRunner.java) | Lanza el proceso con el entorno limpio y recoge el resultado |
| [infrastructure/sandbox/RunnerSandboxExecutor.java](../src/main/java/co/uceva/judge/infrastructure/sandbox/RunnerSandboxExecutor.java) | Escribe el fuente en disco y traduce el resultado a `JudgeResult` |

---

## 12. Limitaciones conocidas

- **Solo Python.** `RunnerSandboxExecutor` rechaza los demás lenguajes con `SandboxExecutionException`; la compilación de C, C++ y Java es otra historia de usuario.
- **Montajes por lenguaje sin resolver.** Java necesitará al menos su configuración de `/etc`, y compilar exige un directorio escribible más generoso.
- **Una sola capa.** Anidar un segundo bwrap se analizó y no se recomienda: ambas capas usarían las mismas primitivas del mismo kernel, así que un fallo del kernel las atravesaría. Lo que sí aporta separar capas —dejar al supervisor fuera del sandbox— ya está hecho. Un caso donde una segunda invocación ayudaría, sin anidar, es usar dos `bwrap` distintos para compilar (más procesos, escritura) y para ejecutar (todo cerrado). No está implementado.
- **Relajaciones del contenedor.** `seccomp`, `apparmor` y `systempaths` en `unconfined` reducen las defensas de Docker. Se compensan con un usuario sin capabilities y con el filtro seccomp del código evaluado, pero una vulnerabilidad del kernel sigue siendo un riesgo compartido por todas las ejecuciones.

---

## 13. Estado de la validación

Lo de este documento se midió en un host real (aarch64, kernel `6.8.0-1060-oracle`, cgroup v2) sobre el **paquete de sandbox `goslint-sandbox`**, un prototipo que aún no forma parte de este repositorio. Su `prueba_humo.sh` pasó 17 de 17:

| Comprobación | Resultado |
|---|---|
| bwrap arranca con `--proc /proc`, seccomp y hoja de cgroup | Correcto |
| `/proc` corresponde a su PID namespace | 2 procesos visibles |
| Filtro seccomp activo | `Seccomp: 2` |
| Sin capabilities efectivas | `CapEff` en cero |
| Entorno mínimo | `LC_CTYPE`, `PATH`, `PWD`; el secreto de prueba no se ve |
| No ve `/cg` ni `/sys/fs/cgroup` | Correcto |
| Único archivo de control expuesto | `/run/cgp` |
| El fd 3 del filtro no se filtra | Correcto |
| `mount()` mata el proceso | Salida 159 (SIGSYS) |
| La hoja contiene solo el programa | Correcto |
| Dos ejecuciones simultáneas no se ven | Correcto |

Pendiente de validar:

- **El comando que construye este servicio.** El prototipo montaba el fuente en `/work`; `BwrapCommandFactory` lo monta en `/solution` y da un `/work` escribible aparte, pensando en la compilación. Esa variante no se ha probado en un host real.
- Control sin `systempaths=unconfined` (sección 5).
- Java y C++ dentro del sandbox con el filtro seccomp, y los montajes extra que necesiten.
- Carga concurrente sostenida.
- Un host x86_64: todo lo anterior se validó solo en aarch64.
