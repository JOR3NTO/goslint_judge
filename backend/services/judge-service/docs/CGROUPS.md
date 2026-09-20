# cgroups v2 en el sandbox — `judge-service`

> Cómo se limita y se mide lo que consume el código de un estudiante, y de dónde salen las cifras con las que se decide el veredicto.
> Documento vivo: actualizar cuando cambie el ciclo de vida de una hoja o la forma de medir.

---

## Tabla de contenido

1. [Qué resuelve](#1-qué-resuelve)
2. [La jerarquía](#2-la-jerarquía)
3. [Por qué la preparación va en el host](#3-por-qué-la-preparación-va-en-el-host)
4. [Los archivos que se usan](#4-los-archivos-que-se-usan)
5. [Ciclo de vida de una hoja](#5-ciclo-de-vida-de-una-hoja)
6. [Qué contiene la hoja](#6-qué-contiene-la-hoja)
7. [Cómo se interpreta cada límite](#7-cómo-se-interpreta-cada-límite)
8. [Aislamiento de las métricas](#8-aislamiento-de-las-métricas)
9. [Limpieza](#9-limpieza)
10. [Topes globales](#10-topes-globales)
11. [Mapa de archivos](#11-mapa-de-archivos)
12. [Contrato del Runner](#12-contrato-del-runner)
13. [Configuración](#13-configuración)
14. [Diagnóstico de errores frecuentes](#14-diagnóstico-de-errores-frecuentes)
15. [Estado de la validación](#15-estado-de-la-validación)

---

## 1. Qué resuelve

Un cgroup v2 es un grupo de procesos al que el kernel le aplica límites y le lleva la contabilidad. En el sandbox cumple dos funciones que bwrap no cubre:

- **Limitar** memoria, cantidad de procesos e hilos.
- **Medir** CPU consumida, pico de memoria y si hubo un OOM-kill, para decidir el veredicto.

Cada ejecución tiene su propia **hoja**, un cgroup creado solo para ella. Así las métricas de una no se mezclan con las de otra, y un exceso de una no afecta a las demás.

El aislamiento de procesos y del sistema de archivos es cosa de bwrap: ver [BWRAP.md](./BWRAP.md). Lo que hay que preparar en el host está en [REQUIREMENTS.md](./REQUIREMENTS.md).

---

## 2. La jerarquía

```
/sys/fs/cgroup                       raíz de cgroups del host
└── goslint.slice                    preparada en el host; en el contenedor es /cg (lectura y escritura)
    ├── docker-<id>.scope            el cgroup del propio contenedor
    ├── prog-<uuid-1>                hoja de la ejecución 1   (la crea SandboxWorkspace con mkdir)
    ├── prog-<uuid-2>                hoja de la ejecución 2
    └── ...
```

Detalle importante: las hojas `prog-*` son **hermanas** del scope del contenedor, no hijas. Cuando un proceso se mueve a una hoja, sale del cgroup del contenedor. Tres consecuencias:

- Los límites del contenedor (`--memory`, `--pids-limit`) **no cubren** a los programas evaluados. Solo los cubren los de su hoja y, opcionalmente, un tope en la slice (sección 10).
- `docker stats` no refleja lo que consumen las evaluaciones.
- Detener el contenedor sí las elimina, porque cada programa vive en un PID namespace dentro del contenedor y ese namespace desaparece con él.

---

## 3. Por qué la preparación va en el host

Para mover un proceso a otro cgroup, el kernel exige permiso de escritura sobre `cgroup.procs` en el **ancestro común** entre el cgroup de origen y el de destino. Aquí el origen es el scope del contenedor, el destino es una hoja, y el ancestro común es la slice. La slice pertenece a root, así que el contenedor no puede crear esa rama por sí mismo desde dentro.

La solución tiene tres piezas:

1. La slice se crea **en el host**, antes que el contenedor, con los controladores `memory` y `pids` habilitados.
2. Docker crea el contenedor directamente dentro de ella con `--cgroup-parent=goslint.slice`, con `--cgroupns=host` para que las rutas coincidan, y monta solo esa rama en `/cg`.
3. El entrypoint del contenedor (como root, antes de bajar al usuario sin privilegios) ejecuta `chown -R` sobre `/cg`. El `-R` importa: sin él, `cgroup.procs` de la slice sigue siendo de root y aparece el error clásico `write error: Permission denied`.

El detalle de cada paso está en [REQUIREMENTS.md](./REQUIREMENTS.md), secciones 4 y 6.

---

## 4. Los archivos que se usan

| Archivo | Uso | Detalle |
|---|---|---|
| `memory.max` | Escritura | Tope de memoria de la hoja |
| `memory.swap.max` | Escritura | Se pone en 0 para que el exceso no se pague en swap |
| `pids.max` | Escritura | Tope de **tareas**: cuenta hilos, no solo procesos |
| `cgroup.procs` | Escritura, desde el sandbox | Único archivo expuesto al programa, como `/run/cgp` |
| `cpu.stat` | Lectura | `usage_usec`: CPU total (usuario y sistema) de la hoja. Existe aunque el controlador `cpu` no esté activo |
| `memory.peak` | Lectura | Pico de memoria de la hoja. Requiere kernel 5.19 |
| `memory.events` | Lectura | Contadores; el que importa es `oom_kill` |
| `pids.events` | Lectura | `max`: veces que un `fork` fue rechazado por llegar a `pids.max` |
| `cgroup.kill` | Escritura | Escribir `1` mata todo el árbol de la hoja de una vez. Requiere kernel 5.14 |

---

## 5. Ciclo de vida de una hoja

Lo reparten [SandboxWorkspace](../src/main/java/co/uceva/judge/infrastructure/sandbox/workspace/SandboxWorkspace.java) (pasos 1 a 3 y 8) y [TestCaseRunner](../src/main/java/co/uceva/judge/infrastructure/sandbox/execution/TestCaseRunner.java) (pasos 4 a 7):

```
 1  mkdir /cg/prog-<uuid>
 2  escribir memory.max, pids.max y memory.swap.max, y releerlos
       si un límite no se aplicó -> no se ejecuta nada
 3  copiar la solución a /work/<uuid>
 4  bwrap arranca con --bind .../cgroup.procs /run/cgp
 5  dentro del sandbox: echo $$ > /run/cgp ; exec programa
       a partir de aquí el programa y sus hijos cuentan en la hoja
 6  el watchdog lee cpu.stat cada app.sandbox.monitor.watch-interval-ms
       CPU sobre el límite duro -> cgroup.kill + destroyForcibly()
 7  el programa termina; se leen cpu.stat, memory.peak y memory.events
 8  cleanup(): borrado de /work/<uuid>, cgroup.kill y rmdir de la hoja con reintentos
```

> Un límite que no se aplica debe abortar la ejecución: nunca se corre código ajeno sin límites efectivos. Por eso el paso 2 los relee antes de seguir, y el paso 8 escribe `cgroup.kill` antes del `rmdir`, porque una hoja con procesos vivos no se puede borrar.

---

## 6. Qué contiene la hoja

Solo el programa evaluado y sus descendientes. El proceso `bwrap` y su init se quedan fuera, en el cgroup del contenedor. Por eso:

- `cpu.stat` mide únicamente el programa: **14 ms** para un `python3` trivial, sin ruido de preparación.
- `memory.peak` no incluye la memoria de bwrap.
- El presupuesto de `pids.max` no se gasta en procesos del sandbox.

Comprobado en el host: mientras una ejecución corre, su `cgroup.procs` contiene un solo PID, el del programa. El `sh` que se enrola pasa unos milisegundos dentro de la hoja antes del `exec`; es despreciable frente a límites de segundos.

---

## 7. Cómo se interpreta cada límite

### Memoria

`memory.max` fija el tope. Cuando un programa lo alcanza, el kernel lo mata (OOM-kill) y el proceso sale con código 137. Dos hechos medidos:

- **`memory.peak` nunca supera `memory.max`.** En la prueba dieron ambos `52428800`. Por eso una condición como `memory.peak > límite` casi nunca se cumple, y un programa que se pasó de memoria terminaría juzgado como `WRONG_ANSWER` con salida vacía. El veredicto `MEMORY_LIMIT_EXCEEDED` debe decidirse con `oom_kill > 0` en `memory.events`, que es lo que hace `TestCaseRunner`. En la prueba: `oom_kill=1`, salida 137.
- **El `/tmp` del sandbox cuenta contra `memory.max`.** Es un tmpfs, así que vive en memoria. Por eso cada `--tmpfs` del comando de bwrap debe declarar su `--size` (ver [BWRAP.md](./BWRAP.md), sección 3).

El límite por problema llega desde `problem-service` como `memoryLimitKb` y se convierte a bytes al llamar al `Runner`.

### Procesos e hilos

`pids.max` cuenta tareas, es decir, procesos e hilos. Con `pids.max=5`, una fork bomb en Python logró **4 forks** y el quinto fue rechazado: `pids.events` registró `max=1`, y el servidor siguió respondiendo con normalidad.

Cinco alcanza para un programa de Python de un solo proceso, y es el valor por defecto de [`PidsLimit`](../src/main/java/co/uceva/judge/domain/valueobject/PidsLimit.java). Un programa multihilo, y sobre todo la JVM, que abre muchos hilos, necesitará bastante más. Ese valor **no se ha medido**: hay que ajustarlo al probar Java.

### CPU

La CPU **no se limita**, se **mide**. El [watchdog](../src/main/java/co/uceva/judge/infrastructure/sandbox/monitor/TimeWatchdog.java) compara `usage_usec` con el límite del problema:

- **Margen duro** (`app.sandbox.monitor.hard-time-percent`, 30 % por defecto): si `usage_usec` lo supera durante la ejecución, el watchdog mata la hoja y el proceso.
- **Al terminar**, `TestCaseRunner` vuelve a comparar el `usage_usec` final con el límite para emitir `TIME_LIMIT_EXCEEDED`.

El tiempo de CPU no es tiempo real: un programa dormido no gasta CPU. Por eso existe además un tope de tiempo real por ejecución (`app.sandbox.monitor.absolute-time-ms`, 10 s por defecto), que debe medirse **con una sola unidad de tiempo**: mezclar milisegundos de calendario con `nanoTime()` da una condición arbitraria.

---

## 8. Aislamiento de las métricas

Desde dentro del sandbox no hay forma de ver ni alterar cgroups:

- No existe `/cg` ni `/sys/fs/cgroup`, y `/run` contiene un solo archivo: `cgp`.
- El programa no puede reescribir `memory.max` ni `pids.max` porque no los ve.
- No puede leer las métricas de otras ejecuciones.

---

## 9. Limpieza

- **Una hoja solo se borra con `rmdir`.** `rm -rf` no sirve: los archivos de control no se pueden eliminar. Además, una hoja con procesos vivos no se puede borrar (`Device or resource busy`).
- El diseño de referencia escribe `1` en `cgroup.kill` y reintenta el `rmdir` durante ~1 s antes de avisar. Una hoja que no se borra suele indicar procesos que siguen vivos.
- El entrypoint del contenedor borra al arrancar las hojas `prog-*` que hayan quedado de una vida anterior.
- Comprobado en el host: tras una fork bomb y un OOM-kill no queda ningún proceso, y todas las hojas se borran con `rmdir`.

---

## 10. Topes globales

Como las hojas cuelgan de la slice y no del contenedor, la protección de todo el servidor depende de lo que se ponga en la slice:

```ini
# /etc/systemd/system/goslint.slice
[Slice]
MemoryMax=...
TasksMax=...
```

Dimensionamiento: cada evaluación concurrente puede llegar a su `memory.max`, más el servicio y el `/work` en memoria. El número de evaluaciones simultáneas por `memory.max` debe caber en el `MemoryMax` de la slice. **Recomendado, no validado.**

---

## 11. Mapa de archivos

| Archivo | Rol |
|---------|-----|
| [infrastructure/sandbox/workspace/SandboxWorkspace.java](../src/main/java/co/uceva/judge/infrastructure/sandbox/workspace/SandboxWorkspace.java) | Crea la hoja, escribe los límites, prepara `/work/<uuid>` y limpia al terminar |
| [infrastructure/sandbox/execution/TestCaseRunner.java](../src/main/java/co/uceva/judge/infrastructure/sandbox/execution/TestCaseRunner.java) | Lanza el proceso, lee las métricas del cgroup y decide el veredicto del caso |
| [infrastructure/sandbox/monitor/TimeWatchdog.java](../src/main/java/co/uceva/judge/infrastructure/sandbox/monitor/TimeWatchdog.java) | Vigila `cpu.stat` y mata la hoja si se pasa de tiempo |
| [infrastructure/sandbox/monitor/OutputHandle.java](../src/main/java/co/uceva/judge/infrastructure/sandbox/monitor/OutputHandle.java) | Consume stdout con tope y mata la hoja si se excede |
| [infrastructure/sandbox/monitor/ErrorsHandle.java](../src/main/java/co/uceva/judge/infrastructure/sandbox/monitor/ErrorsHandle.java) | Consume stderr con tope y detecta errores en ejecución |
| [infrastructure/sandbox/Runner.java](../src/main/java/co/uceva/judge/infrastructure/sandbox/Runner.java) | Recorre los casos de prueba y agrega las métricas máximas |
| [domain/valueobject/](../src/main/java/co/uceva/judge/domain/valueobject/) | `PidsLimit`, `VolumeSizeLimit`, `MemoryLimit`, `TimeLimit` y los límites de los monitores, con sus rangos válidos |

---

## 12. Contrato del Runner

Lo que cualquier implementación debe cumplir:

| Requisito | Motivo |
|---|---|
| Copiar la solución a `/work/<uuid>` antes de lanzar bwrap | El sandbox solo ve esa carpeta |
| Crear la hoja, aplicar límites y **verificarlos leyéndolos** | No ejecutar nada sin límites efectivos |
| `memory.swap.max=0` en cada hoja | Con swap activo, el exceso se pagaría en disco en vez de provocar el OOM-kill |
| Enrolar solo con `--bind .../cgroup.procs /run/cgp` | El código no debe ver `memory.max` ni `pids.max` |
| `--size` en cada `--tmpfs` | Un tmpfs sin tope se dimensiona a la mitad de la RAM |
| Cerrar stdin tras escribir la entrada | Sin EOF, un programa que lee hasta el final no termina |
| Leer stdout mientras el programa corre, con tope, y después `waitFor()` | Con más de 64 KB de salida el pipe se llena y el programa se bloquea |
| Separar stdout de stderr, y drenar ambos hasta EOF | Un traceback no debe contarse como respuesta, ni perderse a medias |
| MLE con `oom_kill`, no con `memory.peak` | Ver sección 7 |
| Watchdog con una sola unidad de tiempo y `destroyForcibly()` | SIGTERM puede ser ignorado, y mezclar relojes da un tope arbitrario |
| Comparar CPU y límite en la misma unidad | `usage_usec` está en µs y el límite del problema llega en ms |
| `cgroup.kill` y `rmdir` con reintentos al limpiar | Una hoja ocupada no se borra al primer intento |
| Borrar la hoja y `/work/<uuid>` siempre, también si hay error | Evita fugas |
| Estado por ejecución, sin campos compartidos | Varias evaluaciones a la vez con una instancia |
| Entorno del proceso vacío (`environment().clear()`) | El programa no debe heredar credenciales |

---

## 13. Configuración

Los límites por problema (`timeLimitMs`, `memoryLimitKb`) llegan desde `problem-service`. Los de los monitores viven en [application.properties](../src/main/resources/application.properties) y un `ADMIN` puede cambiarlos en caliente con `PUT /api/v1/judge/monitor-limits`:

| Propiedad | Por defecto | Rol |
|-----------|-------------|-----|
| `app.sandbox.monitor.watch-interval-ms` | `500` | Cada cuánto el watchdog lee `cpu.stat` |
| `app.sandbox.monitor.hard-time-percent` | `0.3` | Margen sobre el límite de tiempo antes de matar la hoja |
| `app.sandbox.monitor.absolute-time-ms` | `10000` | Tope de tiempo real por ejecución |
| `app.sandbox.monitor.output-size-bytes` | `10485760` | Tope de stdout |
| `app.sandbox.monitor.error-size-bytes` | `10485760` | Tope de stderr |

`PidsLimit` y `VolumeSizeLimit` no son configurables todavía: se declaran con sus valores por defecto en [SandboxConfig](../src/main/java/co/uceva/judge/infrastructure/config/SandboxConfig.java).

---

## 14. Diagnóstico de errores frecuentes

| Síntoma | Causa probable |
|---|---|
| `Permission denied` al escribir `cgroup.procs` | Falta el `chown -R` sobre `/cg`, o el contenedor arrancó sin root y el entrypoint no pudo hacerlo |
| No existe `memory.max` o `pids.max` en la hoja | Los controladores no están habilitados en el `subtree_control` de la slice |
| `Can't find source path .../cgroup.procs` en bwrap | La hoja no es un cgroup v2 real |
| `memory.peak` no existe | Kernel anterior a 5.19 |
| Hojas `prog-*` que se acumulan | Procesos vivos dentro de ellas, o la limpieza no llegó a borrarlas |
| Veredicto `WRONG_ANSWER` con salida vacía tras usar mucha memoria | El MLE se está decidiendo con `memory.peak` en vez de `oom_kill` |
| Mensaje de delegación de cgroups ausente en `docker logs` | La slice no existe o `/cg` no está montado en escritura |

---

## 15. Estado de la validación

Las cifras de este documento se midieron en un host real (aarch64, kernel `6.8.0-1060-oracle`) sobre el **paquete de sandbox `goslint-sandbox`**, un prototipo que todavía no forma parte de este repositorio (ver [REQUIREMENTS.md](./REQUIREMENTS.md), anexo). Su script `prueba_humo.sh` pasó 17 de 17 comprobaciones:

| Comprobación | Resultado |
|---|---|
| La hoja contiene solo el programa | `[python3 prog_<uuid>.py]` |
| CPU de un programa trivial | 14 249 µs |
| Fork bomb con `pids.max=5` | 4 forks; `pids.events max=1` |
| OOM-kill | `oom_kill=1`, salida 137 |
| `memory.peak` frente a `memory.max` | Iguales: 52 428 800 |
| Limpieza | Sin procesos residuales; todas las hojas se borran con `rmdir` |

Pendiente de validar:

- **El código de `judge-service` contra cgroups reales.** Lo medido fue el prototipo; las clases de `infrastructure/sandbox` de este servicio solo tienen pruebas unitarias.
- `cgroup.kill`, `memory.swap.max` y el `rmdir` con reintentos.
- Métricas de una ejecución mientras otra consume CPU a fondo.
- `pids.max` adecuado para la JVM.
- Los topes de la slice (`MemoryMax`, `TasksMax`).
