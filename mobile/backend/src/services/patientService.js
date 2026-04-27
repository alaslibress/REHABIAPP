// Servicio de datos del paciente
// Orquesta llamadas a la API de Java y transforma la respuesta al formato GraphQL
// Los campos clinicos (alergias, antecedentes, medicacion) se filtran aqui — no se exponen al movil
'use strict';

const apiClient = require('./apiClient');
const { crearError } = require('../utils/errors');

/**
 * Obtiene el perfil del paciente y lo transforma al tipo GraphQL Patient.
 * CRITICO: filtra los campos clinicos sensibles que no deben llegar al movil.
 *
 * @param {string} dniPac
 * @param {string|null} javaToken
 * @returns {Promise<object>} Patient
 */
// Limite de tamano de foto: 512 KB en bytes
const FOTO_MAX_BYTES = 512 * 1024;

async function obtenerPerfil(dniPac, javaToken) {
  // Llamadas en paralelo: perfil + foto
  const [data, fotoData] = await Promise.all([
    apiClient.get(`/api/pacientes/${dniPac}`, javaToken),
    apiClient.get(`/api/pacientes/${dniPac}/foto`, javaToken).catch(() => null),
  ]);

  if (!data) {
    throw crearError('PATIENT_NOT_FOUND');
  }

  // Construir data URI si hay foto y no supera el limite de tamano
  let avatarDataUri = null;
  if (fotoData && fotoData.base64) {
    const byteLen = Buffer.byteLength(fotoData.base64, 'base64');
    if (byteLen <= FOTO_MAX_BYTES) {
      const mime = fotoData.mimeType || 'image/png';
      avatarDataUri = `data:${mime};base64,${fotoData.base64}`;
    }
  }

  // Mapeo Java PacienteResponse -> GraphQL Patient
  // Los campos clinicos (alergias, antecedentes, medicacionActual) se omiten
  return {
    id: data.dniPac,
    dni: data.dniPac,
    name: data.nombrePac,
    // Apellido compuesto: primer apellido + segundo (si existe)
    surname: [data.apellido1Pac, data.apellido2Pac].filter(Boolean).join(' '),
    email: data.emailPac || null,
    // Solo el primer telefono de la lista
    phone: data.telefonos && data.telefonos.length > 0 ? data.telefonos[0] : null,
    birthDate: data.fechaNacimiento || null,
    // La direccion es un objeto relacional en Java — no disponible directamente
    address: null,
    active: data.activo,
    numSs: data.numSs || null,
    sexo: data.sexo || null,
    avatarDataUri,
  };
}

/**
 * Obtiene las discapacidades asignadas al paciente.
 *
 * @param {string} dniPac
 * @param {string|null} javaToken
 * @returns {Promise<Array>} Disability[]
 */
async function obtenerDiscapacidades(dniPac, javaToken) {
  const data = await apiClient.get(`/api/pacientes/${dniPac}/discapacidades`, javaToken);
  const lista = Array.isArray(data) ? data : [];

  // Mapeo Java PacienteDiscapacidadResponse -> GraphQL Disability
  return lista.map((d) => ({
    id: d.codDis,
    name: d.nombreDis,
    description: null, // No viene en el DTO de asignacion
    currentLevel: d.idNivel || 0,
  }));
}

/**
 * Obtiene el resumen de progreso terapeutico del paciente.
 * Endpoint pendiente en la API de Java — devuelve datos mock hasta que este disponible.
 *
 * @param {string} dniPac
 * @param {string|null} javaToken
 * @returns {Promise<object>} ProgressSummary
 */
async function obtenerProgreso(dniPac, javaToken) {
  // DEPENDENCIA PENDIENTE: endpoint /api/pacientes/{dniPac}/progreso no existe aun en Java
  // Cuando se implemente, sustituir este mock por la llamada real
  return {
    totalSessions: 0,
    averageScore: null,
    improvementRate: null,
    lastSessionDate: null,
  };
}

module.exports = { obtenerPerfil, obtenerDiscapacidades, obtenerProgreso };
