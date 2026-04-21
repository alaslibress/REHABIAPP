// Punto de entrada del BFF mobile-backend
// Express + Apollo Server 4 + pino + prometheus
'use strict';

const express = require('express');
const pinoHttp = require('pino-http');
const promClient = require('prom-client');
const { ApolloServer } = require('@apollo/server');
const { expressMiddleware } = require('@apollo/server/express4');

const config = require('./config');
const logger = require('./logger');
const typeDefs = require('./graphql/typeDefs/index');
const resolvers = require('./graphql/resolvers/index');
const authMiddleware = require('./middleware/auth');
const greetingMiddleware = require('./middleware/greeting');
const errorFormatter = require('./middleware/errorFormatter');

// ----- Metricas de Prometheus -----
promClient.collectDefaultMetrics();

const app = express();

// Middleware de logging HTTP estructurado
app.use(pinoHttp({ logger }));

// ----- Endpoints de infraestructura (fuera de GraphQL) -----
// Usados por K8s startupProbe, livenessProbe y readinessProbe
app.get('/health', (_req, res) => {
  res.status(200).json({ status: 'UP' });
});

// Metricas de Prometheus para el stack de observabilidad
app.get('/metrics', async (_req, res) => {
  res.set('Content-Type', promClient.register.contentType);
  res.end(await promClient.register.metrics());
});

// ----- Inicio asincrono: Apollo Server requiere server.start() -----
async function iniciar() {
  const server = new ApolloServer({
    typeDefs,
    resolvers,
    formatError: errorFormatter,
    // Introspection solo en desarrollo — no exponer el esquema en produccion (seguridad)
    introspection: config.nodeEnv !== 'production',
  });

  await server.start();
  logger.info({ graphqlPath: config.graphqlPath }, 'Apollo Server iniciado');

  // ----- CORS para el cliente Apollo del frontend movil -----
  app.use(config.graphqlPath, (req, res, next) => {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, X-Timezone');
    res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
    if (req.method === 'OPTIONS') return res.sendStatus(204);
    next();
  });

  // ----- Montar Apollo Server en Express -----
  app.use(
    config.graphqlPath,
    express.json(),
    expressMiddleware(server, {
      context: async ({ req }) => {
        const authCtx = await authMiddleware(req);
        const greeting = greetingMiddleware(req);
        return { ...authCtx, greeting, logger };
      },
    })
  );

  // ----- Inicio del servidor HTTP -----
  // Envolver app.listen en una Promise para garantizar que el servidor este escuchando
  const httpServer = await new Promise((resolve, reject) => {
    const srv = app.listen(config.port, () => {
      logger.info(
        { port: config.port, env: config.nodeEnv, apiBaseUrl: config.apiBaseUrl, mockApi: config.mockApi },
        'BFF mobile-backend iniciado'
      );
      resolve(srv);
    });
    srv.on('error', reject);
  });

  // ----- Apagado graceful: Kubernetes envia SIGTERM antes de terminar el pod -----
  process.on('SIGTERM', () => {
    logger.info('SIGTERM recibido, cerrando servidor...');
    httpServer.close(() => {
      logger.info('Servidor cerrado correctamente');
      process.exit(0);
    });
  });

  return { app, server: httpServer };
}

// Auto-ejecutar e exportar la promesa para los tests
// Los tests esperan esta promesa para obtener { app, server }
const servidorPromise = iniciar().catch((err) => {
  logger.error({ err }, 'Error fatal al iniciar el servidor');
  process.exit(1);
});

module.exports = servidorPromise;
