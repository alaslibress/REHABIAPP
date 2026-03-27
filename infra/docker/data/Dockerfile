# =============================================================================
# Dockerfile para el modulo Data (pipeline de datos Node.js + MongoDB)
# Construccion multi-etapa: instalacion de dependencias de produccion y ejecucion
# Compatible con ENS Alto: usuario no privilegiado, sistema de archivos de solo lectura
# =============================================================================

# --- Etapa 1: Instalacion de dependencias de produccion ---
FROM node:20-alpine AS deps

WORKDIR /app

# Copiar manifiestos de dependencias para aprovechar la cache de capas de Docker
# Las dependencias solo se re-instalan si cambia package.json o package-lock.json
COPY data/package.json .
COPY data/package-lock.json .

# Instalar unicamente dependencias de produccion (sin devDependencies)
RUN npm ci --only=production

# --- Etapa 2: Ejecucion ---
FROM node:20-alpine AS runtime

WORKDIR /app

# Crear grupo y usuario no privilegiado para ejecutar la aplicacion
RUN addgroup -g 1000 appgroup && \
    adduser -u 1000 -G appgroup -s /bin/sh -D appuser

# Crear directorio temporal con propietario correcto
RUN mkdir -p /app/tmp && \
    chown -R appuser:appgroup /app

# Copiar dependencias de produccion desde la etapa anterior
COPY --from=deps --chown=appuser:appgroup /app/node_modules node_modules

# Copiar el codigo fuente del pipeline de datos
COPY --chown=appuser:appgroup data/src src
COPY --chown=appuser:appgroup data/package.json .

# Establecer propietario final del directorio de la aplicacion
RUN chown -R appuser:appgroup /app

# Ejecutar como usuario no privilegiado (requerido por ENS Alto)
USER 1000:1000

# Puerto del servicio de datos
EXPOSE 8081

# Verificacion de salud: comprueba el endpoint de health del servidor
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8081/actuator/health/liveness || exit 1

# Punto de entrada principal del pipeline de datos
ENTRYPOINT ["node", "src/index.js"]
