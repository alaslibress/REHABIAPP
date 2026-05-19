# Dockerfile chatbot WhatsApp (Node 20 + Chromium para whatsapp-web.js)
# whatsapp-web.js usa Puppeteer -> necesita Chromium del sistema.
# Usamos node:20-bullseye-slim porque alpine no es compatible con Puppeteer oficial.

# ---- Stage 1: dependencias ----
FROM node:20-bullseye-slim AS deps
WORKDIR /app
ENV PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true
COPY package.json package-lock.json ./
RUN npm ci --omit=dev

# ---- Stage 2: runtime ----
FROM node:20-bullseye-slim AS runtime
WORKDIR /app

# Chromium del sistema + dependencias minimas requeridas por Puppeteer.
RUN apt-get update && apt-get install -y --no-install-recommends \
    chromium ca-certificates fonts-liberation libnss3 libatk-bridge2.0-0 \
    libxkbcommon0 libxcomposite1 libxdamage1 libxrandr2 libgbm1 libpango-1.0-0 \
    libcairo2 libasound2 dumb-init \
    && rm -rf /var/lib/apt/lists/*

ENV PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true \
    PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium \
    NODE_ENV=production

# Copiar dependencias y codigo fuente.
COPY --from=deps /app/node_modules ./node_modules
COPY package.json ./
COPY src ./src

# Directorio de sesion WhatsApp persistente (montado como volumen).
RUN mkdir -p /app/.wwebjs_auth /app/.wwebjs_cache \
    && chown -R node:node /app

USER node

# Usamos dumb-init para reaper de procesos Chromium zombies.
ENTRYPOINT ["dumb-init", "--", "node", "src/index.js"]
