// Punto de entrada del BFF (Backend-For-Frontend) para la app movil de RehabiAPP
// Todas las peticiones del frontend movil pasan por este servicio antes de llegar a la API central
const express = require('express');
const pino = require('pino');
const pinoHttp = require('pino-http');
const promClient = require('prom-client');
const config = require('./config');

// ----- Configuracion del logger estructurado JSON para Kubernetes -----
const logger = pino({
  level: process.env.LOG_LEVEL || 'info',
  // En produccion pino emite JSON por defecto sin configuracion adicional
});

// ----- Metricas de Prometheus -----
promClient.collectDefaultMetrics();

const app = express();
app.use(express.json());

// Middleware de logging HTTP estructurado (compatible con agregadores de logs en K8s)
app.use(pinoHttp({ logger }));

// ----- Endpoint de salud para probes de Kubernetes -----
// Usado por startupProbe, livenessProbe y readinessProbe definidos en el Deployment
app.get('/health', (req, res) => {
  res.status(200).json({ status: 'UP' });
});

// ----- Endpoint de metricas para Prometheus -----
app.get('/metrics', async (req, res) => {
  res.set('Content-Type', promClient.register.contentType);
  res.end(await promClient.register.metrics());
});

// ----- Inicio del servidor -----
const server = app.listen(config.port, () => {
  logger.info({ port: config.port, env: config.nodeEnv, apiBaseUrl: config.apiBaseUrl }, 'BFF mobile-backend iniciado');
});

// ----- Apagado graceful: Kubernetes envia SIGTERM antes de terminar el pod -----
process.on('SIGTERM', () => {
  logger.info('SIGTERM recibido, cerrando servidor...');
  server.close(() => {
    logger.info('Servidor cerrado correctamente');
    process.exit(0);
  });
});

module.exports = { app, server };
