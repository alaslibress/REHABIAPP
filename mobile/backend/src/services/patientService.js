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
async function obtenerPerfil(dniPac, javaToken) {
  const data = await apiClient.get(`/api/pacientes/${dniPac}`, javaToken);

  if (!data) {
    throw crearError('PATIENT_NOT_FOUND');
  }

  // Foto: el API Java expone GET /api/pacientes/{dni}/foto como bytes binarios
  // (image/png). El movil necesita un data URI base64 para mostrar el avatar
  // en <Image source={{uri: ...}}>. Convertimos aqui.
  // Tolerante a 204 No Content (paciente sin foto): devolvemos null.
  let avatarDataUri = null;
  try {
    const foto = await apiClient.getBinary(`/api/pacientes/${dniPac}/foto`, javaToken);
    if (foto && foto.buffer && foto.buffer.length > 0) {
      const ct = foto.contentType || 'image/png';
      avatarDataUri = `data:${ct};base64,${foto.buffer.toString('base64')}`;
    }
  } catch {
    // Fallo no critico — el avatar no es esencial para mostrar el perfil.
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
 * Obtiene el resumen plano del progreso terapeutico del paciente.
 * Consume el endpoint `GET /api/pacientes/{dni}/progreso/resumen` del API Java
 * (que a su vez proxia al pipeline /data y agrega desde MongoDB).
 *
 * Tolera fallos del upstream devolviendo un resumen vacio para no romper la
 * welcome card del movil cuando /data o Mongo no estan disponibles.
 *
 * @param {string} dniPac
 * @param {string|null} javaToken
 * @returns {Promise<object>} ProgressSummary
 */
async function obtenerProgreso(dniPac, javaToken) {
  try {
    const data = await apiClient.get(`/api/pacientes/${dniPac}/progreso/resumen`, javaToken);
    return {
      totalSessions: data?.totalSessions ?? 0,
      averageScore: data?.averageScore ?? null,
      improvementRate: data?.improvementRate ?? null,
      lastSessionDate: data?.lastSessionDate ?? null,
    };
  } catch (err) {
    // Fallback defensivo: si el API o /data fallan, no rompemos el dashboard.
    return {
      totalSessions: 0,
      averageScore: null,
      improvementRate: null,
      lastSessionDate: null,
    };
  }
}

module.exports = { obtenerPerfil, obtenerDiscapacidades, obtenerProgreso };
