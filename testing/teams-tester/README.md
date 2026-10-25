# Probador de equipos e invitaciones (`contest-service`)

Página temporal para probar a mano las HU de maratones, equipos e invitaciones:
crear concursos, crear equipos (el creador queda como capitán), invitar por correo,
aceptar/rechazar por la API o por los enlaces del correo, el límite de 3 integrantes
y el cierre de inscripciones al iniciar el concurso.

## Requisitos previos

1. Postgres corriendo y las credenciales en `backend/services/problem-service/.env`.
2. Para recibir correos reales, `backend/services/contest-service/.env` con las variables `MAIL_*`
   de Gmail (contraseña de aplicación; el archivo está en el `.gitignore`). `start.sh` lo carga solo.

## Uso

```bash
./testing/teams-tester/start.sh
```

Arranca `problem-service` (`:8082`) y `contest-service` (`:8086`) si no están corriendo
(compilándolos si el código cambió) y luego el probador. Al salir con Ctrl+C detiene solo
lo que arrancó él. Logs en `backend/logs/`.

**Sesión guardada:** los usuarios de prueba, el concurso y el equipo elegidos y lo escrito en
los formularios se guardan en el navegador, así que al recargar la página sigues donde lo dejaste.
**Olvidar sesión** (barra superior) borra el concurso, el equipo y los formularios guardados; los
usuarios de prueba se conservan (**Regenerar todos** los reinicia).

Abre **http://localhost:4300** (no con `file://`: `crypto.subtle` necesita un contexto servido).
No necesita `npm install`: usa solo módulos nativos de Node 20+.

1. **Usuarios de prueba**: se generan un organizador y cuatro estudiantes con su UUID.
   Elige arriba **"Actuar como"**: todas las peticiones salen con un JWT de ese usuario.
   Pon tu correo real en el estudiante al que vayas a invitar para recibir el email.
2. **Escenario rápido** crea un concurso y un equipo capitaneado por el primer estudiante;
   el de **3 problemas con globo** crea un concurso con los 3 primeros problemas de problem-service.
   En **Problemas del concurso** eliges un problema del catálogo (alias, posición y color se sugieren),
   lo asignas, y editas o retiras cada fila en el mismo lugar.
   En **Aclaraciones** (públicas) el actor pregunta sobre un problema, siempre que esté en un equipo del
   concurso, y el organizador responde desde la misma lista.
3. **Invitaciones**: invita como capitán; luego cambia de actor al invitado para
   verlas en "Mis invitaciones" y responder por la API o con el enlace del correo.

El servidor solo escucha en `127.0.0.1`, porque entrega el `JWT_SECRET` al navegador.
