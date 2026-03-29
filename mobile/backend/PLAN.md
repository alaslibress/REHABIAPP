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

- [x] Step 1: Node.js project initialized with Express
- [x] Step 2: /health endpoint returns 200 OK with `{ "status": "UP" }`
- [x] Step 3: Structured JSON logging with pino configured
- [x] Step 4: /metrics endpoint for Prometheus enabled
- [x] Step 5: Secret reading utility for CSI-mounted files created
- [x] Step 6: No writes outside /tmp (read-only filesystem compatible)
- [x] Step 7: package-lock.json exists, src/index.js is entry point
- [x] Step 8: .dockerignore created

---

## Step 9: Containerization and Kubernetes Deployment

This step establishes the Docker containerization and Kubernetes deployment strategy for the Mobile Backend BFF. Base K8s manifests exist at `/infra/k8s/base/mobile-backend/`.

### 9.1: Create Dockerfile at `/mobile/backend/Dockerfile`

Create a multi-stage Node.js Dockerfile at the service root:

**Stage 1 - Dependencies:**
```dockerfile
FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --only=production
```

**Stage 2 - Runtime:**
```dockerfile
FROM node:20-alpine AS runtime
RUN addgroup -g 1000 appgroup && adduser -u 1000 -G appgroup -D -h /app appuser
WORKDIR /app
RUN mkdir -p /app/tmp && chown -R 1000:1000 /app
COPY --from=deps --chown=1000:1000 /app/node_modules ./node_modules
COPY --chown=1000:1000 package.json .
COPY --chown=1000:1000 src ./src
USER 1000:1000
EXPOSE 3000
HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -qO- http://localhost:3000/health || exit 1
ENTRYPOINT ["node", "src/index.js"]
```

**Prerequisite**: The `.dockerignore` from Step 8 must exist. Verify it contains:
```
node_modules
npm-debug.log
.env
.env.*
```

**Note**: This Dockerfile replaces the one at `/infra/docker/mobile-backend/Dockerfile`. Build context is `/mobile/backend/`: `docker build -t mobile-backend:dev -f mobile/backend/Dockerfile mobile/backend/`

### 9.2: Update Deployment to 3 replicas

Modify existing K8s manifests for high availability:

- `/infra/k8s/base/mobile-backend/deployment.yaml`: change `replicas: 2` to `replicas: 3`
- `/infra/k8s/base/mobile-backend/hpa.yaml`: change `minReplicas: 2` to `minReplicas: 3`

### 9.3: Create ConfigMap `mobile-backend-config`

Create `/infra/k8s/base/mobile-backend/configmap.yaml`:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: mobile-backend-config
  namespace: rehabiapp-mobile
  labels:
    app: mobile-backend
    tier: bff
data:
  NODE_ENV: "production"
  PORT: "3000"
  API_BASE_URL: "http://rehabiapp-api.rehabiapp-api.svc.cluster.local:8080"
  LOG_LEVEL: "info"
```

Update `/infra/k8s/base/mobile-backend/kustomization.yaml` to include `- configmap.yaml` in resources list.

### 9.4: Refactor Deployment environment variables

Modify the Deployment container spec to use ConfigMap:

```yaml
envFrom:
  - configMapRef:
      name: mobile-backend-config
```

Remove hardcoded env entries (`NODE_ENV`, `PORT`, `API_BASE_URL`) now provided by ConfigMap.

No Secrets needed in base manifests — the Mobile Backend BFF has no database credentials. In the AWS overlay, the session secret is mounted via CSI SecretProviderClass at `/mnt/secrets/`.

### 9.5: Mobile Backend K8s topology

Complete Kubernetes architecture reference for the implementer:

| Resource | Name | Specification |
|----------|------|---------------|
| Deployment | `mobile-backend` | 3 replicas, port 3000 |
| Service | `mobile-backend` | ClusterIP, port 3000 |
| ConfigMap | `mobile-backend-config` | 4 configuration keys |
| HPA | `mobile-backend` | min 3, max 6 replicas, CPU 70%, memory 80% |
| PDB | `mobile-backend` | minAvailable: 1 |
| NetworkPolicy | `allow-mobile-traffic` | Ingress: ONLY from ingress-nginx on TCP:3000; Egress: ONLY to rehabiapp-api:8080 + kube-dns |
| ServiceAccount | `mobile-backend-sa` | IRSA in AWS overlay |

**CRITICAL**: Mobile-backend NEVER communicates with data service, PostgreSQL, or MongoDB. All traffic routes exclusively to the API service via `API_BASE_URL`.

**Probes:**
- Startup: `GET /health:3000` (initialDelaySeconds: 5, failureThreshold: 10)
- Liveness: `GET /health:3000` (periodSeconds: 10)
- Readiness: `GET /health:3000` (periodSeconds: 5)

**Resources per pod:**
- Requests: 100m CPU, 256Mi memory
- Limits: 500m CPU, 512Mi memory

**Security (ENS Alto):**
- Non-root user UID 1000:1000
- `readOnlyRootFilesystem: true` (emptyDir volume at `/tmp`, 50Mi Memory)
- `allowPrivilegeEscalation: false`
- Drop ALL capabilities
- seccomp profile: RuntimeDefault

### Checklist Step 9

- [x] Step 9.1: Dockerfile created at `/mobile/backend/Dockerfile` (multi-stage, Node.js 20 Alpine, UID 1000)
- [x] Step 9.1: `.dockerignore` verified (from Step 8)
- [x] Step 9.2: Deployment replicas updated from 2 to 3
- [x] Step 9.2: HPA minReplicas updated from 2 to 3
- [x] Step 9.3: ConfigMap `mobile-backend-config` created at `/infra/k8s/base/mobile-backend/configmap.yaml`
- [x] Step 9.3: Kustomization updated to include configmap.yaml
- [x] Step 9.4: Deployment env refactored with `envFrom` ConfigMap
- [ ] Verification: `docker build -t mobile-backend:dev -f mobile/backend/Dockerfile mobile/backend/` succeeds
- [ ] Verification: `kubectl kustomize infra/k8s/overlays/local/` valid
