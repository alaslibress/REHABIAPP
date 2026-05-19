// Servicio de descarga del PDF de un tratamiento
// Proxy a GET /api/tratamientos/{cod}/pdf — el API Java devuelve binario application/pdf
// El BFF serializa los bytes en base64 para el frontend movil (Apollo no maneja binario)
'use strict';

const apiClient = require('./apiClient');
const { crearError } = require('../utils/errors');

// Limite duro: 10 MB. La API Java ya lo aplica al subir; aqui lo replicamos al servir.
const MAX_PDF_BYTES = 10 * 1024 * 1024;

/**
 * Descarga el PDF de un tratamiento desde la API y lo devuelve serializado en base64.
 *
 * @param {string} codTrat - Codigo del tratamiento
 * @param {string|null} javaToken - JWT de la API Java
 * @returns {Promise<{ codTrat, filename, sizeBytes, base64Content }>}
 */
async function obtenerPdfTratamiento(codTrat, javaToken) {
  const { buffer, filename } = await apiClient.getBinary(
    `/api/tratamientos/${codTrat}/pdf`,
    javaToken
  );

  if (!buffer || buffer.length === 0) {
    throw crearError('VALIDATION_ERROR', 'El tratamiento no tiene un PDF asociado.');
  }

  // Tope de seguridad: aunque el API Java limita a 10 MB en upload,
  // verificamos en servida para evitar enviar payloads enormes al movil.
  if (buffer.length > MAX_PDF_BYTES) {
    throw crearError('VALIDATION_ERROR', 'El PDF excede el tamano maximo permitido (10 MB).');
  }

  return {
    codTrat,
    filename: filename || `${codTrat}.pdf`,
    sizeBytes: buffer.length,
    base64Content: buffer.toString('base64'),
  };
}

module.exports = { obtenerPdfTratamiento, MAX_PDF_BYTES };
