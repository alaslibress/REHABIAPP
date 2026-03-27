# PLAN.md — Mobile Backend BFF: Kubernetes Readiness

> **Agent:** Sonnet (Doer) under Agent 2 (Mobile)
> **Domain:** `/mobile/backend/`
> **Prerequisites:** Read `/CLAUDE.md` (root) for BFF pattern rules. Read `/mobile/backend/CLAUDE.md` if it exists.
> **Scope:** Make the Node.js Express BFF container-ready for the K8s manifests defined in `/infra/PLAN.md`.

---

## Context

The mobile backend is a Backend-For-Frontend (BFF) that routes ALL mobile frontend traffic to the Core API. It NEVER communicates with databases, the data pipeline, or any other service — only the Core API.

The `/infra/` K8s manifests expect this service to:
- Run on port 3000
- Expose a `/health` endpoint for probes
- Accept `API_BASE_URL` environment variable for Core API connection
- Accept `NODE_ENV` for environment selection
- Read secrets from CSI-mounted files at `/mnt/secrets/` (AWS)
- Run as UID 1000 with read-only filesystem
- Write temp files only to `/tmp`

---

## Step 1: Initialize Node.js project

If not already initialized:

```bash
cd mobile/backend
npm init -y
npm install express
```

Set in `package.json`:
```json
{
  "name": "rehabiapp-mobile-backend",
  "version": "1.0.0",
  "main": "src/index.js",
  "scripts": {
    "start": "node src/index.js",
    "dev": "node --watch src/index.js"
  }
}
```

---

## Step 2: Create entry point with health endpoint

### `src/index.js`

Create the Express application with:

1. **Health endpoint** at `GET /health` returning `{ "status": "UP" }` with 200 OK. This is used by:
   - K8s startupProbe, livenessProbe, readinessProbe (all target `/health:3000`)
   - Dockerfile HEALTHCHECK

2. **Port configuration** from `process.env.PORT` defaulting to 3000.

3. **API base URL** from `process.env.API_BASE_URL` defaulting to `http://localhost:8080`. ALL outbound requests proxy to this URL.

4. **Graceful shutdown** handling SIGTERM (K8s sends this before pod termination):
   ```javascript
   process.on('SIGTERM', () => {
     server.close(() => process.exit(0));
   });
   ```

---

## Step 3: Configure structured JSON logging

K8s skill mandates structured JSON logs to stdout.

### 3.1 Install pino (lightweight JSON logger)

```bash
npm install pino pino-http
```

### 3.2 Configure in `src/index.js`

```javascript
const pino = require('pino');
const pinoHttp = require('pino-http');

const logger = pino({
  level: process.env.LOG_LEVEL || 'info',
  // In production, pino outputs JSON by default (no config needed)
  // In development, use pino-pretty (devDependency only)
});

app.use(pinoHttp({ logger }));
```

For local development, add `pino-pretty` as devDependency:
```bash
npm install -D pino-pretty
```

---

## Step 4: Configure Prometheus metrics endpoint

### 4.1 Install prom-client

```bash
npm install prom-client
```

### 4.2 Add `/metrics` endpoint

```javascript
const promClient = require('prom-client');
promClient.collectDefaultMetrics();

app.get('/metrics', async (req, res) => {
  res.set('Content-Type', promClient.register.contentType);
  res.end(await promClient.register.metrics());
});
```

---

## Step 5: Configure secret reading for AWS

In AWS, secrets are mounted as files at `/mnt/secrets/`. Create a utility to read them:

### `src/config.js`

```javascript
const fs = require('fs');
const path = require('path');

function readSecret(name, fallback) {
  const secretPath = path.join(process.env.SECRETS_DIR || '/mnt/secrets', name);
  try {
    return fs.readFileSync(secretPath, 'utf8').trim();
  } catch {
    return fallback || process.env[name.toUpperCase().replace(/-/g, '_')];
  }
}

module.exports = {
  port: parseInt(process.env.PORT || '3000', 10),
  apiBaseUrl: process.env.API_BASE_URL || 'http://localhost:8080',
  sessionSecret: readSecret('session-secret', 'dev-session-secret'),
  nodeEnv: process.env.NODE_ENV || 'development',
};
```

---

## Step 6: Ensure filesystem compatibility

The K8s deployment uses `readOnlyRootFilesystem: true` with an emptyDir at `/tmp`.

- Do NOT write to any directory other than `/tmp`.
- Do NOT use `fs.writeFileSync` to paths outside `/tmp`.
- If the app needs a cache directory, use `/tmp/cache`.
- `npm ci --only=production` in the Dockerfile excludes devDependencies — ensure no devDependency is required at runtime.

---

## Step 7: Verify Dockerfile compatibility

The Dockerfile at `infra/docker/mobile-backend/Dockerfile` expects:
- `mobile/backend/package.json` and `mobile/backend/package-lock.json` to exist.
- `mobile/backend/src/` to contain the application source.
- Entry point: `node src/index.js`.

Ensure:
- `package-lock.json` exists (run `npm install` to generate it).
- All source code lives under `src/`.
- No build step is needed (plain JS, no TypeScript compilation).

---

## Step 8: Create .dockerignore

### `mobile/backend/.dockerignore`

```
node_modules
npm-debug.log
.env
.env.*
```

This prevents copying `node_modules` into the Docker build context (they are installed fresh via `npm ci`).

---

## Checklist

- [ ] Step 1: Node.js project initialized with Express
- [ ] Step 2: /health endpoint returns 200 OK with `{ "status": "UP" }`
- [ ] Step 3: Structured JSON logging with pino configured
- [ ] Step 4: /metrics endpoint for Prometheus enabled
- [ ] Step 5: Secret reading utility for CSI-mounted files created
- [ ] Step 6: No writes outside /tmp (read-only filesystem compatible)
- [ ] Step 7: package-lock.json exists, src/index.js is entry point
- [ ] Step 8: .dockerignore created
