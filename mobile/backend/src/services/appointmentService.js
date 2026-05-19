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
    // CitaResponse del API trae `nombreSanitario` enriquecido (nombre + apellidos).
    // Mantenemos fallback al DNI por si el contrato cambia o el sanitario fue dado de baja.
    practitionerName: cita.nombreSanitario || cita.dniSan,
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
  // Usa el endpoint del API que devuelve TODAS las citas del paciente
  // (pasadas y futuras), paginado. Antes pedia /api/citas?fecha=<hoy> y
  // filtraba — eso solo mostraba citas del dia actual, asi que el historial
  // movil salia vacio practicamente siempre.
  const hoy = new Date().toISOString().split('T')[0];
  // Sort usa `id.fechaCita` / `id.horaCita` — la PK es @EmbeddedId CitaId,
  // Spring Data no acepta `fechaCita` directo en el sort param.
  const resp = await apiClient.get(
    `/api/citas/paciente/${encodeURIComponent(dniPac)}?page=0&size=500&sort=id.fechaCita,asc&sort=id.horaCita,asc`,
    javaToken,
  );

  // /api responde con PageResponse {contenido, totalElementos, ...} o array
  // directo segun version; aceptamos ambos shapes.
  let lista;
  if (Array.isArray(resp)) {
    lista = resp;
  } else if (resp && Array.isArray(resp.contenido)) {
    lista = resp.contenido;
  } else if (resp && Array.isArray(resp.content)) {
    lista = resp.content;
  } else {
    lista = [];
  }

  // Filtro defensivo — el API ya filtra por DNI, esto previene fugas si cambia el contrato.
  lista = lista.filter((c) => c.dniPac === dniPac);

  // Filtrar por horizonte temporal (proximas/pasadas).
  if (filtros.upcoming === true) {
    lista = lista.filter((c) => c.fechaCita >= hoy);
  } else if (filtros.upcoming === false) {
    lista = lista.filter((c) => c.fechaCita < hoy);
  }

  // Filtro por estado (Java aun no expone estado — solo SCHEDULED disponible).
  if (filtros.status && filtros.status !== 'SCHEDULED') {
    return [];
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

/**
 * Crea una solicitud de cita (no una cita confirmada).
 * En mock: genera un id sintieticamente y devuelve estado PENDING.
 * En produccion: POST /api/citas/solicitudes (endpoint pendiente en /api Phase 12).
 *
 * @param {string} dniPac
 * @param {{ fechaPreferida, horaPreferida, motivo, telefono?, email? }} args
 * @param {string|null} _javaToken
 * @returns {Promise<object>} AppointmentRequest
 */
async function solicitarCita(dniPac, args, _javaToken) {
  // Validacion ligera (la API Java validara con mas detalle cuando se conecte real).
  if (!args.motivo || args.motivo.trim().length < 5) {
    const { crearError: crearErrorLocal } = require('../utils/errors');
    throw crearErrorLocal('VALIDATION_ERROR');
  }

  // Id sintietico estable: dni + timestamp.
  const id = `REQ-${dniPac}-${Date.now()}`;
  return {
    id,
    fechaPreferida: args.fechaPreferida,
    horaPreferida: args.horaPreferida,
    motivo: args.motivo,
    estado: 'PENDING',
    createdAt: new Date().toISOString(),
  };
}

module.exports = { obtenerCitas, reservarCita, cancelarCita, solicitarCita };
