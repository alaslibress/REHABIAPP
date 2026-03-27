# =============================================================================
# Dockerfile para el modulo Mobile Backend (BFF - Backend for Frontend Node.js)
# Construccion multi-etapa: instalacion de dependencias de produccion y ejecucion
# Compatible con ENS Alto: usuario no privilegiado, sistema de archivos de solo lectura
# =============================================================================

# --- Etapa 1: Instalacion de dependencias de produccion ---
FROM node:20-alpine AS deps

WORKDIR /app

# Copiar manifiestos de dependencias para aprovechar la cache de capas de Docker
# Las dependencias solo se re-instalan si cambia package.json o package-lock.json
COPY mobile/backend/package.json .
COPY mobile/backend/package-lock.json .

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

# Copiar el codigo fuente de la aplicacion BFF
COPY --chown=appuser:appgroup mobile/backend/src src
COPY --chown=appuser:appgroup mobile/backend/package.json .

# Establecer propietario final del directorio de la aplicacion
RUN chown -R appuser:appgroup /app

# Ejecutar como usuario no privilegiado (requerido por ENS Alto)
USER 1000:1000

# Puerto del BFF (Backend for Frontend) movil
EXPOSE 3000

# Verificacion de salud: comprueba el endpoint de health del servidor Express
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:3000/health || exit 1

# Punto de entrada principal de la aplicacion Node.js BFF
ENTRYPOINT ["node", "src/index.js"]
