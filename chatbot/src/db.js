'use strict';
const { Pool } = require('pg');
const config = require('./config');
const logger = require('./logger');

const pool = new Pool(config.pg);

pool.on('error', function (err) {
  logger.error({ err: err.message }, 'Error inesperado en pool PostgreSQL');
});

// Busca un paciente por su numero de telefono (formato libre, igualamos por sufijo).
// telefono_paciente almacena el telefono tal y como el paciente lo registro:
// puede traer prefijo internacional o no. Hacemos match por LIKE sobre los
// ultimos 9 digitos para tolerar variaciones.
async function buscarPacientePorTelefono(telefonoE164) {
  const sufijo = telefonoE164.replace(/\D/g, '').slice(-9);
  const { rows } = await pool.query(
    `SELECT p.dni_pac, p.dni_san, p.nombre_pac, p.apellido1_pac, p.activo
     FROM paciente p
     INNER JOIN telefono_paciente tp ON tp.dni_pac = p.dni_pac
     WHERE regexp_replace(tp.telefono, '\\D', '', 'g') LIKE '%' || $1
       AND p.activo = TRUE
     LIMIT 1`,
    [sufijo]
  );
  return rows[0] || null;
}

// Busca un paciente por DNI exacto.
async function buscarPacientePorDni(dni) {
  const { rows } = await pool.query(
    `SELECT dni_pac, dni_san, nombre_pac, apellido1_pac, activo
     FROM paciente
     WHERE dni_pac = $1 AND activo = TRUE
     LIMIT 1`,
    [dni]
  );
  return rows[0] || null;
}

// Comprueba si el slot (dni_san, fecha, hora) esta libre.
async function slotDisponible(dniSan, fecha, hora) {
  const { rows } = await pool.query(
    `SELECT 1 FROM cita WHERE dni_san = $1 AND fecha_cita = $2 AND hora = $3 LIMIT 1`,
    [dniSan, fecha, hora]
  );
  return rows.length === 0;
}

// Inserta una cita. Devuelve true si OK, false si ya existia (unique violation).
async function insertarCita(dniPac, dniSan, fecha, hora) {
  try {
    await pool.query(
      `INSERT INTO cita (dni_pac, dni_san, fecha_cita, hora) VALUES ($1, $2, $3, $4)`,
      [dniPac, dniSan, fecha, hora]
    );
    return true;
  } catch (err) {
    if (err.code === '23505') return false; // unique violation
    throw err;
  }
}

// Registra en audit_log usando el schema real de V1+V12:
//   accion IN ('LOGIN','LOGOUT','CREATE','READ','UPDATE','SOFT_DELETE','DELETE','EXPORT','PRINT','CAMBIO_CONTRASENA')
//   dni_usuario VARCHAR(20) — usamos config.auditActor ('CHATBOT_BOT')
//   entidad VARCHAR(100) — 'cita'
//   id_entidad VARCHAR(200) — dniPac
//   detalle TEXT — JSON con canal, fecha, hora, dni_san
// Si el INSERT falla, se loguea y NO se lanza — la cita ya fue creada.
async function registrarAudit(actor, dniPac, detalles) {
  try {
    await pool.query(
      `INSERT INTO audit_log (dni_usuario, nombre_usuario, accion, entidad, id_entidad, detalle)
       VALUES ($1, $2, 'CREATE', 'cita', $3, $4)`,
      [actor, 'WhatsApp Chatbot', dniPac, JSON.stringify(detalles)]
    );
  } catch (err) {
    logger.warn({ err: err.message }, 'Audit log fallo — cita ya creada, continuando');
  }
}

module.exports = {
  buscarPacientePorTelefono,
  buscarPacientePorDni,
  slotDisponible,
  insertarCita,
  registrarAudit,
  pool,
};
