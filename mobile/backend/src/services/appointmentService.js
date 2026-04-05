// Servicio de citas medicas del paciente
// Orquesta las operaciones de cita contra la API de Java
'use strict';

const apiClient = require('./apiClient');
const { crearError } = require('../utils/errors');

// Separador para el ID compuesto de citas: dniPac_dniSan_fecha_hora
const SEP = '__';

/**
 * Construye un ID sintetico para una cita a partir de sus 4 campos.
 *
 * @param {string} dniPac
 * @param {string} dniSan
 * @param {string} fecha - formato yyyy-MM-dd
 * @param {string} hora - formato HH:mm:ss
 * @returns {string}
 */
function construirId(dniPac, dniSan, fecha, hora) {
  return `${dniPac}${SEP}${dniSan}${SEP}${fecha}${SEP}${hora}`;
}

/**
 * Descompone un ID sintetico de cita en sus partes.
 *
 * @param {string} id
 * @returns {{ dniPac, dniSan, fecha, hora }}
 */
function descomponerIdCita(id) {
  const partes = id.split(SEP);
  if (partes.length !== 4) {
    throw crearError('APPOINTMENT_NOT_FOUND');
  }
  return { dniPac: partes[0], dniSan: partes[1], fecha: partes[2], hora: partes[3] };
}

/**
 * Transforma un CitaResponse de Java al tipo GraphQL Appointment.
 * NOTA: La API de Java no tiene campo 'status' ni 'notes' en CitaResponse.
 * El campo practitionerName usa el DNI como placeholder hasta que Java enriquezca el DTO.
 *
 * @param {object} cita - CitaResponse de Java
 * @returns {object} Appointment
 */
function transformarCita(cita) {
  const horaFormateada = cita.horaCita
    ? cita.horaCita.substring(0, 5) // HH:mm desde HH:mm:ss
    : cita.horaCita;

  return {
    id: construirId(cita.dniPac, cita.dniSan, cita.fechaCita, cita.horaCita),
    date: cita.fechaCita,
    time: horaFormateada,
    // DEPENDENCIA PENDIENTE: nombre del sanitario requiere llamada adicional a /api/sanitarios/{dniSan}
    practitionerName: cita.dniSan,
    practitionerSpecialty: null,
    status: 'SCHEDULED',
    notes: null,
  };
}

/**
 * Obtiene las citas del paciente con filtros opcionales.
 * LIMITACION: La API de Java no tiene endpoint por paciente, solo por fecha.
 * El BFF obtiene las citas del dia actual + proximos 30 dias y filtra por dniPac.
 *
 * @param {string} dniPac
 * @param {string|null} javaToken
 * @param {{ status?: string, upcoming?: boolean }} filtros
 * @returns {Promise<Array>} Appointment[]
 */
async function obtenerCitas(dniPac, javaToken, filtros = {}) {
  const hoy = new Date().toISOString().split('T')[0];
  const data = await apiClient.get(`/api/citas?fecha=${hoy}`, javaToken);
  let lista = Array.isArray(data) ? data : [];

  // Filtrar por el paciente autenticado
  lista = lista.filter((c) => c.dniPac === dniPac);

  // Filtrar solo citas proximas
  if (filtros.upcoming === true) {
    lista = lista.filter((c) => c.fechaCita >= hoy);
  }

  // Filtrar por estado (en Java no existe estado — solo SCHEDULED disponible)
  if (filtros.status && filtros.status !== 'SCHEDULED') {
    return []; // Solo SCHEDULED disponible por ahora
  }

  return lista.map(transformarCita);
}

/**
 * Reserva una nueva cita medica.
 *
 * @param {string} dniPac
 * @param {string} fecha - yyyy-MM-dd
 * @param {string} hora - HH:mm
 * @param {string} practitionerId - DNI del sanitario
 * @param {string|null} javaToken
 * @returns {Promise<object>} Appointment
 */
async function reservarCita(dniPac, fecha, hora, practitionerId, javaToken) {
  const body = {
    dniPac,
    dniSan: practitionerId,
    fechaCita: fecha,
    horaCita: hora.length === 5 ? `${hora}:00` : hora, // Asegurar formato HH:mm:ss
  };

  const cita = await apiClient.post('/api/citas', body, javaToken);
  return transformarCita(cita);
}

/**
 * Cancela una cita existente por su ID sintetico.
 *
 * @param {string} appointmentId - ID sintetico generado por el BFF
 * @param {string|null} javaToken
 * @returns {Promise<object>} { id, status: 'CANCELLED' }
 */
async function cancelarCita(appointmentId, javaToken) {
  const { dniPac, dniSan, fecha, hora } = descomponerIdCita(appointmentId);

  await apiClient.delete('/api/citas', { dniPac, dniSan, fecha, hora }, javaToken);

  return {
    id: appointmentId,
    date: fecha,
    time: hora.substring(0, 5),
    practitionerName: dniSan,
    practitionerSpecialty: null,
    status: 'CANCELLED',
    notes: null,
  };
}

module.exports = { obtenerCitas, reservarCita, cancelarCita };
