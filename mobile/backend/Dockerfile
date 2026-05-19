# Dockerfile del BFF mobile-backend (Node.js 20 Alpine)
# Multi-stage: stage 1 instala dependencias, stage 2 ejecuta en imagen minima
# Cumple ENS Alto: usuario no-root UID 1000, readOnlyRootFilesystem

# ---- Stage 1: Dependencias de produccion ----
FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --only=production

# ---- Stage 2: Runtime ----
FROM node:20-alpine AS runtime
# Reutilizamos el usuario 'node' (uid/gid 1000) provisto por la imagen oficial.
# Cumple ENS Alto: contenedor corre como no-root sin colisiones de UID.
WORKDIR /app
# Directorio temporal necesario por readOnlyRootFilesystem
RUN mkdir -p /app/tmp && chown -R node:node /app
# Copiar dependencias y codigo fuente con permisos del usuario de aplicacion
COPY --from=deps --chown=node:node /app/node_modules ./node_modules
COPY --chown=node:node package.json .
COPY --chown=node:node src ./src
USER node
EXPOSE 3000
# Verificacion de salud usando wget (disponible en Alpine sin dependencias adicionales)
HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -qO- http://localhost:3000/health || exit 1
ENTRYPOINT ["node", "src/index.js"]
