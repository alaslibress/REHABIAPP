# =============================================================================
# Dockerfile para el modulo API (Spring Boot)
# Construccion multi-etapa: compilacion con JDK, ejecucion con JRE minimo
# Compatible con ENS Alto: usuario no privilegiado, sistema de archivos de solo lectura
# =============================================================================

# --- Etapa 1: Compilacion ---
FROM eclipse-temurin:24-jdk-alpine AS builder

WORKDIR /build

# Copiar el wrapper y la configuracion de Maven primero para aprovechar la cache de capas
# Las dependencias solo se re-descargan si cambia pom.xml
COPY api/mvnw .
COPY api/.mvn .mvn
COPY api/pom.xml .

# Descargar dependencias en capa separada (cache de Docker)
RUN ./mvnw dependency:go-offline -B

# Copiar el codigo fuente y compilar el artefacto final
COPY api/src src
RUN ./mvnw clean package -DskipTests -B

# --- Etapa 2: Ejecucion ---
FROM eclipse-temurin:24-jre-alpine AS runtime

# Crear grupo y usuario no privilegiado para ejecutar la aplicacion
RUN addgroup -g 1000 appgroup && \
    adduser -u 1000 -G appgroup -s /bin/sh -D appuser

# Crear directorios de escritura permitidos con propietario correcto
RUN mkdir -p /app/tmp /app/cache && \
    chown -R 1000:1000 /app

# Copiar el JAR compilado desde la etapa de construccion
COPY --from=builder --chown=1000:1000 /build/target/*.jar /app/app.jar

# Ejecutar como usuario no privilegiado (requerido por ENS Alto)
USER 1000:1000

# Puerto de la API de Spring Boot
EXPOSE 8080

# Verificacion de salud: comprueba el endpoint de liveness de Spring Actuator
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health/liveness || exit 1

# Configuraciones JVM optimizadas para contenedores:
# -XX:+UseContainerSupport: detecta limites de CPU/memoria del contenedor
# -XX:MaxRAMPercentage=75.0: usa hasta el 75% de la RAM asignada al contenedor
# -Djava.io.tmpdir: redirige escrituras temporales al directorio permitido
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.io.tmpdir=/app/tmp", \
    "-jar", "/app/app.jar"]
