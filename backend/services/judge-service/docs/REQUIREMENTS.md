# Requisitos del host para el sandbox — `judge-service`

> Lo que hay que preparar **fuera** del código para que la evaluación en sandbox funcione: kernel, cgroups y flags del contenedor.
> Documento vivo: actualizar cuando cambien los requisitos del host o el empaquetado del sandbox.

**Alcance.** Aquí está solo lo que **no se puede resolver desde la imagen**. Instalar paquetes, generar el filtro seccomp o preparar permisos dentro del contenedor es cosa del [Dockerfile](../docker/Dockerfile) y del [entrypoint](../docker/entrypoint.sh) (ver el [anexo](#anexo-qué-resuelve-la-imagen-y-qué-el-host)).

**Referencia técnica.** El porqué de cada requisito está en [BWRAP.md](./BWRAP.md) (aislamiento de procesos y sistema de archivos) y en [CGROUPS.md](./CGROUPS.md) (límites y métricas). Aquí solo está el «qué hace falta» y el «cómo verificarlo».

---

## Tabla de contenido

1. [Kernel Linux 5.19 o superior](#1-kernel-linux-519-o-superior)
2. [cgroup v2 en jerarquía unificada](#2-cgroup-v2-en-jerarquía-unificada)
3. [Docker Engine nativo](#3-docker-engine-nativo)
4. [Slice `goslint.slice` preparada y persistente](#4-slice-goslintslice-preparada-y-persistente)
5. [Flags obligatorios del contenedor](#5-flags-obligatorios-del-contenedor)
6. [Delegación recursiva del cgroup](#6-delegación-recursiva-del-cgroup)
7. [Modelo de ejecución: contenedor persistente](#7-modelo-de-ejecución-contenedor-persistente)
8. [Coordinación de límites de memoria](#8-coordinación-de-límites-de-memoria)
9. [Restricción de user namespaces en Ubuntu 24.04](#9-restricción-de-user-namespaces-en-ubuntu-2404)
10. [Estado de la validación y pendientes](#10-estado-de-la-validación-y-pendientes)

---

## Checklist de verificación rápida

Antes de dar por listo un host, estos comandos deben pasar todos:

```bash
uname -r                                                  # >= 5.19
stat -fc %T /sys/fs/cgroup                                # cgroup2fs
docker info --format '{{.CgroupDriver}}'                  # systemd
systemctl is-active goslint.slice                         # active
cat /sys/fs/cgroup/goslint.slice/cgroup.subtree_control   # debe incluir memory y pids
docker version --format '{{.Server.KernelVersion}}'       # igual que uname -r
```

Con el contenedor arriba:

```bash
docker logs goslint-judge | grep "delegacion de cgroups OK"
docker exec -u ubuntu goslint-judge python3 /opt/judge/gen_seccomp.py -o /tmp/f.bpf --verify
docker exec -u ubuntu goslint-judge sh /opt/judge/prueba_humo.sh       # 17 OK, 0 FALLO
docker inspect goslint-judge --format '{{.HostConfig.Privileged}} {{.HostConfig.CapAdd}}'   # false []
```

Si alguno falla, ver la sección correspondiente.

## Entorno en el que se validó

| Elemento | Valor |
|---|---|
| Arquitectura | aarch64 |
| Kernel del host | `6.8.0-1060-oracle` |
| cgroup | v2 (`cgroup2fs`) |
| Imagen base | `ubuntu:24.04` |
| Capabilities efectivas del usuario | 0 (`CapEff: 0000000000000000`) |
| `Privileged` / `CapAdd` | `false` / `[]` |

> Lo validado fue el prototipo del sandbox. Su `Dockerfile`, entrypoint, generador de seccomp y pruebas de humo ya están integrados en [`services/judge-service/docker/`](../docker/), pero la imagen empaqueta ahora además el propio `judge-service`, y **esa imagen todavía no se ha ejecutado sobre una `goslint.slice` real**.

**No validado:** hosts x86_64, Ubuntu 22.04 con HWE, Docker Desktop, y cualquier carga con Java o C++.

---

## 1. Kernel Linux 5.19 o superior

**Por qué:** `memory.peak` se añadió al controlador de memoria de cgroup v2 en la versión **5.19**. En kernels anteriores ese archivo no existe dentro del cgroup, sin importar permisos, flags de Docker o capabilities. No es un problema de configuración: es una función que el kernel no tiene. `TestCaseRunner` lee `memory.peak` en cada caso de prueba, así que sin él la evaluación falla.

`cgroup.kill`, que se usa para terminar de golpe todo lo que quede en una hoja, requiere 5.14: queda cubierto.

**Verificación:**

```bash
uname -r      # 5.19 o superior (6.x en un host actualizado)
```

**Combinaciones que NO cumplen:**

| Sistema | Kernel | `memory.peak` |
|---|---|---|
| Ubuntu 20.04 (GA o HWE) | 5.4 / 5.15 | No existe |
| Ubuntu 22.04 **GA** | 5.15 | No existe |
| Ubuntu 22.04 **+ HWE** | 6.5 / 6.8 | Disponible |
| Ubuntu 24.04 (GA) | 6.8 | Disponible |

> **El caso más confuso:** subir de Ubuntu 20.04 a 22.04 **no basta**. El kernel GA de 22.04 sigue siendo 5.15; hay que instalar el kernel HWE después del upgrade de distro:
>
> ```bash
> sudo apt install linux-generic-hwe-22.04 -y && sudo reboot
> ```

**Si el host no puede cumplirlo**, la alternativa sería medir el pico por muestreo de `memory.current`. **No está implementado**, y ese modo puede perderse picos más cortos que el intervalo de muestreo.

---

## 2. cgroup v2 en jerarquía unificada

**Por qué:** toda la medición (`cgroup.procs`, `memory.max`, `pids.max`, `cpu.stat`, `memory.events`) asume la API de cgroup v2. Con cgroup v1 o en modo híbrido, las rutas y la semántica son distintas y nada de esto funciona.

**Verificación:**

```bash
stat -fc %T /sys/fs/cgroup     # debe decir: cgroup2fs
```

Si dice `tmpfs`, el host está en cgroup v1 o híbrido.

**Corrección:** en Ubuntu 22.04+ la jerarquía unificada es el valor por defecto. Si está desactivada es porque alguien añadió `systemd.unified_cgroup_hierarchy=0` a los parámetros de arranque: quitarlo de `/etc/default/grub`, correr `sudo update-grub` y reiniciar.

---

## 3. Docker Engine nativo

**Por qué:** Docker Desktop corre el Engine dentro de una VM Linux separada. Los PIDs y cgroups que se ven desde la terminal del host **no son los mismos** que los del Engine, lo que rompe la preparación de la rama de cgroups. En producción debe ser Docker Engine sobre el kernel del host, con el plugin `docker compose` (v2).

**Verificación:**

```bash
docker info --format '{{.CgroupDriver}}'              # esperado: systemd
docker version --format '{{.Server.KernelVersion}}'   # debe coincidir con uname -r
```

Si el `KernelVersion` no coincide, hay Docker Desktop o una VM intermedia.

> **Sobre el driver de cgroups:** el valor esperado en Ubuntu es `systemd`. Con `cgroupfs` (más común en Arch o en Desktop) todo sigue funcionando, pero la slice se prepara con `mkdir` manual en vez de con un unit file (sección 4.2).

---

## 4. Slice `goslint.slice` preparada y persistente

**Por qué:** el contenedor no puede crear su propia rama de cgroups delegada desde dentro: el kernel exige permiso de escritura en el **ancestro común** entre origen y destino, y ese ancestro pertenece a root. La única forma limpia es preparar la rama **desde el host, como root, antes de que el contenedor exista**, y pedirle a Docker que cree el contenedor directamente ahí con `--cgroup-parent`. Las hojas de cada ejecución quedan como **hermanas** del cgroup del contenedor ([CGROUPS.md](./CGROUPS.md), sección 2).

**Es un requisito del host, no de la imagen:** un `mkdir` en `/sys/fs/cgroup` desde el `Dockerfile` no sirve; ese sistema de archivos es virtual y el contenedor ni existe cuando la rama debe estar lista.

### 4.1. Como unit file de systemd (recomendado)

Un `mkdir` manual **se pierde en cada reinicio del host**. Un unit file persiste, y el repositorio trae uno listo en [infrastructure/systemd/goslint.slice](../../../../infrastructure/systemd/goslint.slice):

```bash
sudo cp infrastructure/systemd/goslint.slice /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now goslint.slice
```

Trae comentado un bloque `[Slice]` con `MemoryMax` y `TasksMax` para poner un tope global a todas las ejecuciones (recomendado, sin validar; ver [CGROUPS.md](./CGROUPS.md), sección 10).

**Verificación:**

```bash
systemctl is-active goslint.slice                          # active
cat /sys/fs/cgroup/goslint.slice/cgroup.subtree_control    # debe incluir memory y pids
```

Controladores necesarios: `memory` y `pids`. El controlador `cpu` no es imprescindible para medir, porque `cpu.stat` existe aunque `cpu` no esté habilitado.

### 4.2. Alternativa manual (solo hosts de prueba)

```bash
sudo mkdir -p /sys/fs/cgroup/goslint.slice
echo "+cpu +memory +pids +io" | sudo tee /sys/fs/cgroup/cgroup.subtree_control
echo "+cpu +memory +pids +io" | sudo tee /sys/fs/cgroup/goslint.slice/cgroup.subtree_control
```

> **Nota de shell:** usar `| sudo tee archivo`, nunca `sudo echo ... > archivo`. La redirección `>` la abre tu shell antes de que `sudo` entre en juego, y falla con `Permiso denegado` aunque el `echo` corra como root.

**Hay que repetirlo en cada reinicio del host.** Por eso 4.1 es lo recomendado para producción.

### 4.3. Orden de arranque con Docker (recomendado, no validado)

Si el contenedor se reinicia solo (`restart: unless-stopped`), `docker.service` puede arrancar antes que `goslint.slice` tras un reinicio. Un drop-in evita la carrera:

```ini
# /etc/systemd/system/docker.service.d/goslint.conf
[Unit]
After=goslint.slice
Requires=goslint.slice
```

---

## 5. Flags obligatorios del contenedor

**Por qué:** ninguno se puede poner en el `Dockerfile`: son decisiones del runtime, no de la imagen. Sin ellos, `bwrap` falla antes de ejecutar nada.

Ya están declarados en el servicio `judge-service` de [infrastructure/docker/docker-compose.yml](../../../../infrastructure/docker/docker-compose.yml), bajo el perfil `sandbox` para que un `docker compose up` normal siga levantando solo PostgreSQL y RabbitMQ:

```bash
JWT_SECRET=<mínimo 32 bytes> docker compose --profile sandbox up -d --build judge-service
```

Equivalente con `docker run`:

```bash
docker run -d \
  --name goslint-judge \
  --init \
  --cgroupns=host \
  --cgroup-parent=goslint.slice \
  -v /sys/fs/cgroup/goslint.slice:/cg:rw \
  --tmpfs /work:mode=0755,uid=1000,gid=1000,size=256m \
  --security-opt seccomp=unconfined \
  --security-opt apparmor=unconfined \
  --security-opt systempaths=unconfined \
  -e JWT_SECRET=... -p 8084:8084 \
  goslint-judge:dev
```

| Flag | Por qué es obligatorio |
|---|---|
| `--cgroupns=host` | Docker no soporta montar `/sys/fs/cgroup` como `rw` con `--cgroupns=private` sin `--privileged` (el flag `unmask=` es de Podman y nunca se implementó en Docker; issue moby/moby#46763, abierto) |
| `--cgroup-parent=goslint.slice` | Coloca el contenedor **dentro** de la rama preparada, evitando el problema del «ancestro común» al migrar procesos. Lo hace `dockerd` como root, así que no hay chequeo de permisos que falle |
| `-v /sys/fs/cgroup/goslint.slice:/cg:rw` | Da escritura sobre **una sola rama** del cgroupfs, sin exponer todo `/sys/fs/cgroup` ni requerir `--privileged`. En compose, con `create_host_path: false` para que falle en claro si la slice no existe |
| `--security-opt seccomp=unconfined` | El perfil por defecto de Docker bloquea `mount`, `pivot_root`, `unshare`, `setns` y `umount2`. Sin esto, `bwrap` falla con `pivot_root: Permission denied`. No se resuelve añadiendo capabilities. El código del usuario **no** queda sin filtro: bwrap le carga el suyo |
| `--security-opt apparmor=unconfined` | El perfil `docker-default` incluye una regla `deny mount,` que bloquea la propagación de montajes de `bwrap` (`Failed to make / slave: Permission denied`). Necesario en Ubuntu y Debian |
| `--security-opt systempaths=unconfined` | Quita las máscaras de `/proc` que Docker aplica por defecto. Sin esto el kernel rechaza `bwrap --proc /proc` (sección 7) |
| `--tmpfs /work:...size=256m` | Directorios de trabajo por ejecución (`/work/<uuid>`) en memoria: no dejan basura y se pierden al reiniciar |
| `--init` | `tini` como PID 1, que recoge procesos zombi |

> **Lo que NO hace falta:** ninguna capability añadida (`--cap-add`) y **nunca** `--privileged`. Si alguien propone `--privileged` «para que funcione», es señal de que falta alguno de los flags de arriba.

**Alternativa a `apparmor=unconfined`:** escribir un perfil AppArmor propio que permita explícitamente las operaciones de `mount` con flags de propagación (`rslave`, `private,rec`) que necesita `bwrap`, manteniendo el confinamiento para todo lo demás. Es más trabajo y hay que mantenerlo. No está implementado.

---

## 6. Delegación recursiva del cgroup

**Por qué:** el bind mount de `/cg` llega al contenedor con los archivos de control propiedad de `root`. Para mover procesos a las hojas, el kernel exige permiso de escritura en el cgroup destino **y en el ancestro común**, que aquí es `/cg`.

**El error clásico si falta:** `write error: Permission denied` al escribir en `cgroup.procs`, aunque el directorio hoja sea del usuario.

**Cómo se resuelve:** el entrypoint corre como root al arrancar, ejecuta `chown -R` sobre `/cg` (el `-R` es lo importante), comprueba que el usuario sin privilegios puede crear una hoja y mover un proceso a ella, y baja privilegios.

**Requisitos que impone:**

- El contenedor debe arrancar como **root** (sin `--user` ni `user:`), para que el entrypoint pueda hacer el `chown` antes de bajar de privilegios. Si se arranca ya sin privilegios, hay que hacer el `chown` desde fuera con `docker exec -u 0`.
- Como `docker exec` entra como root por defecto, las pruebas manuales que deban parecerse al servicio se lanzan con `-u <usuario>`. Con root, bwrap se comporta distinto y los resultados no valen.

> **Orden importante:** si la preparación de la slice se automatiza, el `chown -R` va **después** de habilitar `subtree_control`, no antes: al activar controladores el kernel puede crear archivos de control nuevos con el propietario por defecto.

**Verificación (dentro del contenedor, como el usuario del servicio):**

```bash
docker exec -u ubuntu goslint-judge sh -c '
  ls -la /cg | grep cgroup.procs
  mkdir /cg/prueba && echo $$ > /cg/prueba/cgroup.procs && echo OK
  rmdir /cg/prueba'
```

---

## 7. Modelo de ejecución: contenedor persistente

**Decisión de diseño:** un **contenedor persistente** que aloja el servicio, con un `bwrap` nuevo por cada ejecución. No se crea un contenedor por evaluación, no se usa Docker dentro de Docker y no se destruye el contenedor después de cada corrida.

**El problema original.** Con `--bind /proc /proc`, el programa evaluado veía el `/proc` del contenedor: los procesos de otras ejecuciones y del servicio, y podía leer su `cmdline`, `status` y `cgroup`. Era una fuga real.

**Por qué `--proc /proc` fallaba.** El kernel no deja montar un procfs nuevo desde un user namespace si el `/proc` existente tiene partes tapadas, y Docker tapa varias rutas por defecto. **No se arregla con capabilities.**

**Qué lo resolvió.** `--security-opt systempaths=unconfined` quita esas máscaras sin `--privileged`. Con él, cada ejecución recibe un procfs limpio de su propio PID namespace.

**Otras fugas que el rediseño cierra:**

| Fuga anterior | Cierre |
|---|---|
| El código podía reescribir `memory.max` y `pids.max` de su hoja | Solo se expone `cgroup.procs` (`/run/cgp`) |
| `--ro-bind / /` mostraba todo el sistema de archivos del contenedor | Lista blanca: `/usr`, `/proc`, `/dev`, el fuente de esa ejecución y sus tmpfs |
| El código heredaba las variables de entorno del servicio | `--clearenv` en bwrap y `environment().clear()` en `TestCaseRunner` |
| El código corría sin filtro de syscalls | Filtro seccomp cargado por bwrap (`--seccomp 3`) |
| La hoja incluía a bwrap y contaminaba `cpu.stat` y `memory.peak` | bwrap queda fuera de la hoja; solo se enrola el programa |

**Riesgos que se aceptan conscientemente:**

- `seccomp`, `apparmor` y `systempaths` en `unconfined` relajan las defensas del contenedor. Se compensan con un usuario sin capabilities y con el filtro seccomp del código evaluado. Una vulnerabilidad del kernel sigue siendo un riesgo compartido por todas las ejecuciones.
- Las ejecuciones comparten el kernel y el contenedor. El aislamiento entre ellas se apoya en namespaces, montajes, seccomp y cgroups; no en una frontera de contenedor.

---

## 8. Coordinación de límites de memoria

El `--size` de cada `--tmpfs` de `bwrap` y el `memory.max` de la hoja **compiten por el mismo presupuesto de RAM**: un tmpfs vive en memoria, así que lo que el programa escriba cuenta contra el límite del cgroup.

Un `--tmpfs` sin `--size` se dimensiona por defecto a **la mitad de la RAM del host**, lo que permite a un programa agotar la memoria escribiendo archivos. `--size` debe **preceder** al `--tmpfs` y solo afecta al inmediato:

```bash
--size 52428800 --tmpfs /work      # 50 MiB, en bytes
```

El `/work` en memoria del contenedor (256 MB en el ejemplo de la sección 5) es aparte: pertenece al contenedor, no a las hojas.

Con swap activo hay que escribir además `memory.swap.max=0` en cada hoja, para que el exceso no se pague en disco en vez de provocar el OOM-kill ([CGROUPS.md](./CGROUPS.md), sección 12).

---

## 9. Restricción de user namespaces en Ubuntu 24.04

Ubuntu 24.04 puede restringir la creación de user namespaces sin privilegios mediante AppArmor (`kernel.apparmor_restrict_unprivileged_userns`), y `bwrap` depende de ellos.

**Estado:** en el host validado, bwrap funcionó con la configuración de la sección 5. No se registró el valor de este parámetro; conviene anotarlo en cada instalación.

```bash
sysctl kernel.apparmor_restrict_unprivileged_userns     # puede no existir en otros sistemas
```

Si bwrap falla al crear el namespace (por ejemplo, `setting up uid map: Permission denied`) en un host nuevo, este es el primer sitio donde mirar.

---

## 10. Estado de la validación y pendientes

**Comprobado** en un host aarch64 con kernel `6.8.0-1060-oracle`, sobre el prototipo `goslint-sandbox`: la imagen construye, la delegación de cgroups funciona, el filtro seccomp pasa 19 de 19, las pruebas de humo dan 17 de 17, y el contenedor no es privilegiado ni tiene capabilities añadidas.

**Pendiente:**

1. **Levantar la imagen sobre una `goslint.slice` real** y repetir `prueba_humo.sh`. La imagen construye y su filtro seccomp pasa las 19 comprobaciones, pero el código de `infrastructure/sandbox` nunca se ha ejecutado contra cgroups de verdad.
2. **Control de `systempaths=unconfined`:** quitar el flag, recrear el contenedor y repetir las pruebas. Si el primer check falla al montar `/proc`, el flag es necesario; si pasa, se puede eliminar.
3. Java y C++ dentro del sandbox, con el filtro seccomp y los montajes que necesiten.
4. Métricas de una ejecución mientras otra consume CPU a fondo, y carga concurrente sostenida.
5. `pids.max` adecuado para la JVM.
6. Topes globales en la slice (`MemoryMax`, `TasksMax`) y el drop-in de arranque de Docker.
7. Un host x86_64.
8. Perfil AppArmor propio como alternativa a `apparmor=unconfined`.

---

## Anexo: qué resuelve la imagen y qué el host

| Lo resuelve | Qué |
|---|---|
| [docker/Dockerfile](../docker/Dockerfile) | Compila el jar del servicio, instala `bubblewrap`, `python3` y `python3-seccomp`, genera `/opt/judge/filter.bpf` y crea `/work` |
| [docker/entrypoint.sh](../docker/entrypoint.sh) | `chown -R` de `/cg`, limpieza de hojas de corridas previas (con `rmdir`), autodiagnóstico de la delegación y bajada de privilegios |
| [infrastructure/docker/docker-compose.yml](../../../../infrastructure/docker/docker-compose.yml) | Los flags de la sección 5, en el perfil `sandbox` |
| [docker/gen_seccomp.py](../docker/gen_seccomp.py) | El filtro seccomp y su verificación (`--verify`) |
| [docker/prueba_humo.sh](../docker/prueba_humo.sh) | Validación del sandbox real: aislamiento, seccomp, límites y limpieza |
| Cualquier lenguaje adicional | Dependencias en la imagen y, si hace falta, montajes extra en el comando de bwrap |
