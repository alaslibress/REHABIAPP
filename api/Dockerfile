# Dockerfile multi-stage para la API REST de RehabiAPP
# Stage 1: Builder — compila el JAR con Maven
# Stage 2: Runtime — imagen minima de produccion con usuario no-root

# ===== STAGE 1: BUILDER =====
FROM eclipse-temurin:24-jdk-alpine AS builder
WORKDIR /build

# Copiar wrapper de Maven y descriptor de proyecto primero (cache de dependencias)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Descargar dependencias en modo offline para aprovechar cache de capas Docker
RUN ./mvnw dependency:go-offline -B

# Copiar el codigo fuente y compilar el JAR final
COPY src src
RUN ./mvnw clean package -DskipTests -B

# ===== STAGE 2: RUNTIME =====
FROM eclipse-temurin:24-jre-alpine AS runtime

# Crear grupo y usuario no-root (UID/GID 1000) segun ENS Alto
RUN addgroup -g 1000 appgroup && adduser -u 1000 -G appgroup -D -h /app appuser

WORKDIR /app

# Crear directorios de escritura necesarios para readOnlyRootFilesystem
RUN mkdir -p /app/tmp /app/cache && chown -R 1000:1000 /app

# Copiar el JAR construido en el stage anterior
COPY --from=builder --chown=1000:1000 /build/target/*.jar /app/app.jar

# Ejecutar como usuario no-root
USER 1000:1000

# Puerto de escucha de la API
EXPOSE 8080

# Probe de salud integrada en la imagen
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health/liveness || exit 1

# Punto de entrada con flags JVM optimizados para contenedores
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.io.tmpdir=/app/tmp", \
  "-jar", "app.jar"]
