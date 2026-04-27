// Servicio de documentos clinicos (PDF de tratamientos)
'use strict';

const apiClient = require('./apiClient');
const { crearError } = require('../utils/errors');

/**
 * Obtiene el documento PDF asociado a un tratamiento del paciente.
 * Devuelve base64 inline o una URL firmada segun lo que devuelva la API de Java.
 *
 * @param {string} dniPac
 * @param {string} codTrat
 * @param {string|null} javaToken
 * @returns {Promise<object>} TreatmentDocument
 */
async function obtenerDocumentoTratamiento(dniPac, codTrat, javaToken) {
  const res = await apiClient.get(
    `/api/pacientes/${dniPac}/tratamientos/${codTrat}/documento`,
    javaToken
  );

  if (!res) {
    throw crearError('DOCUMENT_DOWNLOAD_FAILED');
  }

  return {
    fileName: res.fileName,
    mimeType: res.mimeType,
    base64: res.base64 ?? null,
    url: res.url ?? null,
  };
}

module.exports = { obtenerDocumentoTratamiento };
