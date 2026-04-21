// Configuracion centralizada del BFF
// Los secretos se leen desde archivos CSI montados en /mnt/secrets/ (AWS)
// Si no existe el archivo, se usa la variable de entorno como fallback
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
  // JWT propio del BFF — DIFERENTE del JWT de la API de Java
  jwtSecret: readSecret('jwt-secret', 'dev-jwt-secret-cambiar-en-produccion'),
  jwtExpirationMs: parseInt(process.env.JWT_EXPIRATION_MS || '1800000', 10),
  jwtRefreshMs: parseInt(process.env.JWT_REFRESH_MS || '604800000', 10),
  // Modo mock: no hace peticiones reales a la API de Java
  mockApi: process.env.MOCK_API === 'true',
  graphqlPath: process.env.GRAPHQL_PATH || '/graphql',
};
