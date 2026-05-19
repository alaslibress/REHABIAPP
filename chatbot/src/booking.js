'use strict';
const config = require('./config');
const db = require('./db');
const llm = require('./llm');
const sessions = require('./sessions');
const { ERROR_REPLIES } = require('./promptTemplates');
const logger = require('./logger');

// Comprueba si la fecha es un dia laborable segun WORKING_DAYS (ISO 1=lun..7=dom).
function esDiaLaborable(fechaIso) {
  const d = new Date(fechaIso + 'T00:00:00');
  const iso = d.getDay() === 0 ? 7 : d.getDay(); // JS: 0=dom, ISO: 7=dom
  return config.reglas.workingDays.includes(iso);
}

// Comprueba si la hora cae en horario [open, close).
function enHorario(horaHHMM) {
  return horaHHMM >= config.reglas.openTime && horaHHMM < config.reglas.closeTime;
}

// Comprueba que la cita es futura y con suficiente antelacion.
function tieneAntelacionSuficiente(fechaIso, horaHHMM) {
  const cita = new Date(`${fechaIso}T${horaHHMM}:00`);
  const minimo = Date.now() + config.reglas.minLeadHours * 60 * 60 * 1000;
  return cita.getTime() >= minimo;
}

// Identifica al paciente: primero por telefono, luego por DNI si la sesion lo pidio.
async function identificar(phoneE164, mensaje) {
  const s = sessions.obtener(phoneE164);

  if (s.pacienteDni) {
    return await db.buscarPacientePorDni(s.pacienteDni);
  }

  // Intentar match por telefono
  const porTel = await db.buscarPacientePorTelefono(phoneE164);
  if (porTel) {
    sessions.actualizar(phoneE164, { pacienteDni: porTel.dni_pac });
    return porTel;
  }

  // Si no hay match y el usuario ya escribio un DNI, intentarlo
  const dniMatch = mensaje.match(/\b(\d{8}[A-Za-z])\b/);
  if (dniMatch) {
    const dni = dniMatch[1].toUpperCase();
    const porDni = await db.buscarPacientePorDni(dni);
    if (porDni) {
      sessions.actualizar(phoneE164, { pacienteDni: porDni.dni_pac, pendienteIdentificarDni: false });
      return porDni;
    }
    return { __noEncontrado: 'dni' };
  }

  // No hay match y no hay DNI en el mensaje — pedirlo
  sessions.actualizar(phoneE164, { pendienteIdentificarDni: true });
  return { __noEncontrado: 'tel' };
}

// Procesa un mensaje entrante. Devuelve la respuesta del bot (string).
async function manejarMensaje(phoneE164, texto) {
  sessions.anadirTurno(phoneE164, 'user', texto);

  // 1) Identificar al paciente
  const paciente = await identificar(phoneE164, texto);
  if (paciente && paciente.__noEncontrado === 'tel') {
    sessions.anadirTurno(phoneE164, 'assistant', ERROR_REPLIES.PACIENTE_NO_ENCONTRADO_TEL);
    return ERROR_REPLIES.PACIENTE_NO_ENCONTRADO_TEL;
  }
  if (paciente && paciente.__noEncontrado === 'dni') {
    sessions.anadirTurno(phoneE164, 'assistant', ERROR_REPLIES.PACIENTE_NO_ENCONTRADO_DNI);
    return ERROR_REPLIES.PACIENTE_NO_ENCONTRADO_DNI;
  }
  if (!paciente) {
    sessions.anadirTurno(phoneE164, 'assistant', ERROR_REPLIES.ERROR_INTERNO);
    return ERROR_REPLIES.ERROR_INTERNO;
  }

  // 2) Pasar el mensaje a Qwen para extraer intent + fecha + hora
  const sesion = sessions.obtener(phoneE164);
  const intencion = await llm.extraerIntencion(sesion.historial.slice(0, -1), texto);

  // 3) Branch por intent
  if (intencion.intent !== 'book') {
    sessions.anadirTurno(phoneE164, 'assistant', intencion.respuesta_usuario);
    return intencion.respuesta_usuario;
  }

  // 4) Si falta fecha u hora, pedirla
  if (!intencion.fecha || !intencion.hora) {
    sessions.anadirTurno(phoneE164, 'assistant', intencion.respuesta_usuario);
    return intencion.respuesta_usuario;
  }

  // 5) Validar reglas de negocio
  if (!esDiaLaborable(intencion.fecha) || !enHorario(intencion.hora)) {
    sessions.anadirTurno(phoneE164, 'assistant', ERROR_REPLIES.FUERA_HORARIO);
    return ERROR_REPLIES.FUERA_HORARIO;
  }
  if (!tieneAntelacionSuficiente(intencion.fecha, intencion.hora)) {
    sessions.anadirTurno(phoneE164, 'assistant', ERROR_REPLIES.POCA_ANTELACION);
    return ERROR_REPLIES.POCA_ANTELACION;
  }

  // 6) Comprobar disponibilidad y reservar
  const horaSql = `${intencion.hora}:00`;
  const libre = await db.slotDisponible(paciente.dni_san, intencion.fecha, horaSql);
  if (!libre) {
    sessions.anadirTurno(phoneE164, 'assistant', ERROR_REPLIES.SLOT_OCUPADO);
    return ERROR_REPLIES.SLOT_OCUPADO;
  }

  const ok = await db.insertarCita(paciente.dni_pac, paciente.dni_san, intencion.fecha, horaSql);
  if (!ok) {
    sessions.anadirTurno(phoneE164, 'assistant', ERROR_REPLIES.SLOT_OCUPADO);
    return ERROR_REPLIES.SLOT_OCUPADO;
  }

  await db.registrarAudit(config.auditActor, paciente.dni_pac, {
    canal: 'whatsapp',
    fecha: intencion.fecha,
    hora: horaSql,
    dni_san: paciente.dni_san,
  });

  const reply = ERROR_REPLIES.CITA_OK(intencion.fecha, intencion.hora, paciente.dni_san);
  sessions.anadirTurno(phoneE164, 'assistant', reply);
  logger.info({ from: phoneE164, fecha: intencion.fecha, hora: horaSql }, 'Cita creada via chatbot');
  return reply;
}

module.exports = { manejarMensaje, esDiaLaborable, enHorario, tieneAntelacionSuficiente };
