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
# Crear usuario y grupo no-root para cumplir con ENS Alto
RUN addgroup -g 1000 appgroup && adduser -u 1000 -G appgroup -D -h /app appuser
WORKDIR /app
# Directorio temporal necesario por readOnlyRootFilesystem
RUN mkdir -p /app/tmp && chown -R 1000:1000 /app
# Copiar dependencias y codigo fuente con permisos del usuario de aplicacion
COPY --from=deps --chown=1000:1000 /app/node_modules ./node_modules
COPY --chown=1000:1000 package.json .
COPY --chown=1000:1000 src ./src
USER 1000:1000
EXPOSE 3000
# Verificacion de salud usando wget (disponible en Alpine sin dependencias adicionales)
HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -qO- http://localhost:3000/health || exit 1
ENTRYPOINT ["node", "src/index.js"]
