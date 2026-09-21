# syntax=docker/dockerfile:1
#
# Imagen del sandbox de judge-service.
#
# El contexto de build es backend/, porque el jar se compila aqui dentro y la
# compilacion necesita el proyecto Gradle completo (shared/ incluido):
#
#   docker build -f services/judge-service/docker/Dockerfile backend/
#
# Requisitos del host y flags obligatorios del contenedor: ../docs/REQUIREMENTS.md

# --------------------------------------------------------------------------
# 1) Compilacion del servicio
# --------------------------------------------------------------------------
FROM eclipse-temurin:17-jdk AS build

WORKDIR /src

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY shared ./shared
COPY services ./services

# El bootJar deja dos archivos en build/libs (el ejecutable y el -plain);
# se copia solo el ejecutable, con un nombre fijo que no dependa de la version.
RUN ./gradlew --no-daemon :services:judge-service:bootJar \
    && find services/judge-service/build/libs -name '*.jar' ! -name '*-plain.jar' \
         -exec cp {} /judge-service.jar \;

# --------------------------------------------------------------------------
# 2) Imagen de ejecucion
# --------------------------------------------------------------------------
FROM ubuntu:24.04

# Herramientas de diagnostico para la fase de pruebas (ps, pgrep, strace).
# En produccion: build sin este argumento (por defecto false).
ARG WITH_DEBUG_TOOLS=false

# bubblewrap: el sandbox de cada ejecucion.
# python3: interprete de las soluciones evaluadas, y el unico lenguaje soportado hoy.
# python3-seccomp: lo usa gen_seccomp.py para construir el filtro.
# openjdk-17-jre-headless: para ejecutar el propio judge-service (no las soluciones).
RUN apt-get update && apt-get install -y --no-install-recommends \
        bubblewrap \
        python3 \
        python3-seccomp \
        openjdk-17-jre-headless \
    && if [ "$WITH_DEBUG_TOOLS" = "true" ]; then \
         apt-get install -y --no-install-recommends procps strace; \
       fi \
    && rm -rf /var/lib/apt/lists/*

# Filtro seccomp (lista negra) para el codigo evaluado. Se genera AQUI, en la
# imagen, porque el .bpf depende de la arquitectura y de la version de libseccomp.
# La ruta es la que espera SandboxWorkspace: /opt/judge/filter.bpf.
COPY services/judge-service/docker/gen_seccomp.py /opt/judge/gen_seccomp.py
RUN python3 /opt/judge/gen_seccomp.py -o /opt/judge/filter.bpf \
    && chmod 0644 /opt/judge/filter.bpf /opt/judge/gen_seccomp.py

# Validacion del sandbox real desde dentro del contenedor:
#   docker exec -u ubuntu goslint-judge sh /opt/judge/prueba_humo.sh
COPY services/judge-service/docker/prueba_humo.sh /opt/judge/prueba_humo.sh
RUN chmod 0755 /opt/judge/prueba_humo.sh

COPY --from=build /judge-service.jar /opt/judge/judge-service.jar

# Directorio donde SandboxWorkspace crea /work/<uuid> por cada ejecucion.
# En compose se monta como tmpfs; aqui se deja creado para un 'docker run' simple.
RUN mkdir -p /work && chown ubuntu:ubuntu /work

# NOTA: /cg NO se toca aqui. Es un bind mount que solo existe en runtime, asi que
# cualquier chown del build queda tapado por el montaje. Lo resuelve entrypoint.sh.
COPY services/judge-service/docker/entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod 0755 /usr/local/bin/entrypoint.sh

EXPOSE 8084

# El entrypoint corre como root SOLO para preparar /cg y luego baja a 'ubuntu'
# (por eso no hay USER aqui). Consecuencia: 'docker exec' entra como root por
# defecto; para pruebas usa siempre:  docker exec -u ubuntu ...
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
CMD ["java", "-jar", "/opt/judge/judge-service.jar"]
