# syntax=docker/dockerfile:1
#
# Imagen genérica para los servicios Spring Boot (auth, problem, submission...).
# Un solo Dockerfile para todos: lo único que cambia es el argumento SERVICE.
#
# El contexto de build es backend/, porque la compilación necesita el proyecto
# Gradle completo (shared/ incluido):
#
#   docker build -f docker/spring-service.Dockerfile --build-arg SERVICE=auth-service backend/
#
# (judge-service tiene su propio Dockerfile porque lleva el sandbox.)

# --------------------------------------------------------------------------
# 1) Compilación: aquí se genera el .jar. Esta etapa pesa mucho (JDK + Gradle +
#    dependencias) y NO viaja a la imagen final.
# --------------------------------------------------------------------------
FROM eclipse-temurin:17-jdk AS build

ARG SERVICE

WORKDIR /src

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY shared ./shared
COPY services ./services

# Defensa para builds hechos desde Windows: si el checkout dejó gradlew con CRLF,
# Linux falla con "bad interpreter". Ver también /.gitattributes.
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# --mount=type=cache: Gradle guarda sus dependencias en /root/.gradle. Con la caché
# montada, la segunda compilación (y la de los otros servicios) no vuelve a
# descargar Spring, Hibernate, etc.
# bootJar deja dos archivos en build/libs (el ejecutable y el -plain): se copia solo
# el ejecutable, con un nombre fijo.
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon :services:${SERVICE}:bootJar -x test \
    && find services/${SERVICE}/build/libs -name '*.jar' ! -name '*-plain.jar' \
         -exec cp {} /app.jar \;

# --------------------------------------------------------------------------
# 2) Ejecución: solo el JRE (sin compilador ni Gradle) y el .jar.
# --------------------------------------------------------------------------
FROM eclipse-temurin:17-jre

# Usuario sin privilegios: si alguien explota el servicio, no es root en el contenedor.
RUN groupadd --system app && useradd --system --gid app --no-create-home app

WORKDIR /app
COPY --from=build /app.jar /app/app.jar

USER app

# Dimensiona la heap según la memoria que le dé Docker al contenedor, en vez de un
# valor fijo. Se puede ampliar desde compose con JAVA_OPTS.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

# "exec" hace que java sea el PID 1 y reciba SIGTERM al hacer 'docker stop'
# (apagado ordenado de Spring) en vez de que lo reciba el shell.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
