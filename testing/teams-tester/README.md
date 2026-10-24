# Probador de equipos e invitaciones (`contest-service`)

Página temporal para probar a mano las HU de maratones, equipos e invitaciones:
crear concursos, crear equipos (el creador queda como capitán), invitar por correo,
aceptar/rechazar por la API o por los enlaces del correo, el límite de 3 integrantes
y el cierre de inscripciones al iniciar el concurso.

## Requisitos previos

1. Postgres corriendo (`goslint-postgres`, puerto 5433).
2. `contest-service` arrancado en `:8086` con el mismo `JWT_SECRET` que
   `backend/services/submission-service/.env`. Para recibir correos reales, con las
   variables `MAIL_*` de Gmail (ver `application.properties`).

## Uso

```bash
./testing/teams-tester/start.sh
```

Abre **http://localhost:4300** (no con `file://`: `crypto.subtle` necesita un contexto servido).
No necesita `npm install`: usa solo módulos nativos de Node 20+.

1. **Usuarios de prueba**: se generan un organizador y cuatro estudiantes con su UUID.
   Elige arriba **"Actuar como"**: todas las peticiones salen con un JWT de ese usuario.
   Pon tu correo real en el estudiante al que vayas a invitar para recibir el email.
2. **Escenario rápido** crea un concurso y un equipo capitaneado por el primer estudiante.
3. **Invitaciones**: invita como capitán; luego cambia de actor al invitado para
   verlas en "Mis invitaciones" y responder por la API o con el enlace del correo.

El servidor solo escucha en `127.0.0.1`, porque entrega el `JWT_SECRET` al navegador.
