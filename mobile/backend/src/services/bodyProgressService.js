// Servicio de progreso por parte del cuerpo
// En modo mock: deriva los datos de las discapacidades + tratamientos del paciente.
// En produccion: consumira un endpoint enriquecido de /api/pacientes/{dni}/progreso/body-parts
// que aun no existe (a implementar en /api Phase 12).
'use strict';

const apiClient = require('./apiClient');

// Mapa codDis -> partes del cuerpo afectadas. Espejo de la logica que /desktop usa
// para colorear el diagrama. Lista cerrada — anadir nuevos codigos cuando aparezcan.
const PARTES_POR_DISCAPACIDAD = {
  M16: ['LEFT_HIP', 'RIGHT_HIP'],
  M54: ['TORSO'],
  M75: ['LEFT_SHOULDER', 'RIGHT_SHOULDER'],
  G56: ['LEFT_HAND', 'RIGHT_HAND'],
};

// Etiqueta legible por parte del cuerpo. Usada como `name` en el GraphQL response.
const ETIQUETAS_PARTE = {
  HEAD: 'Cabeza',
  NECK: 'Cuello',
  TORSO: 'Espalda y tronco',
  LEFT_SHOULDER: 'Hombro izquierdo',
  RIGHT_SHOULDER: 'Hombro derecho',
  LEFT_ARM: 'Brazo izquierdo',
  RIGHT_ARM: 'Brazo derecho',
  LEFT_HAND: 'Mano izquierda',
  RIGHT_HAND: 'Mano derecha',
  LEFT_HIP: 'Cadera izquierda',
  RIGHT_HIP: 'Cadera derecha',
  LEFT_LEG: 'Pierna izquierda',
  RIGHT_LEG: 'Pierna derecha',
  LEFT_FOOT: 'Pie izquierdo',
  RIGHT_FOOT: 'Pie derecho',
};

const TODAS_LAS_PARTES = Object.keys(ETIQUETAS_PARTE);

/**
 * Calcula el resumen de progreso por parte del cuerpo del paciente.
 * Itera sobre TODAS_LAS_PARTES para garantizar que el frontend recibe el set completo
 * (BodyDiagram pinta partes sin tratamiento en gris claro).
 *
 * @param {string} dniPac
 * @param {string|null} javaToken
 * @returns {Promise<Array>} BodyPartProgress[]
 */
async function obtenerProgresoPorParte(dniPac, javaToken) {
  const discapacidades = await apiClient.get(`/api/pacientes/${dniPac}/discapacidades`, javaToken);
  const lista = Array.isArray(discapacidades) ? discapacidades : [];

  // Set de partes del cuerpo afectadas por las discapacidades activas
  const partesAfectadas = new Set();
  for (const d of lista) {
    const partes = PARTES_POR_DISCAPACIDAD[d.codDis] || [];
    for (const p of partes) partesAfectadas.add(p);
  }

  // Hash deterministico para que el progreso mock sea estable entre llamadas.
  // Real /api Phase 12 sustituira esto por valores agregados de MongoDB.
  function hashEstable(parte) {
    let h = 0;
    for (const c of `${dniPac}-${parte}`) h = (h * 31 + c.charCodeAt(0)) >>> 0;
    return h;
  }

  return TODAS_LAS_PARTES.map((parte) => {
    const tieneTratamiento = partesAfectadas.has(parte);
    const seed = hashEstable(parte);
    return {
      id: parte,
      name: ETIQUETAS_PARTE[parte],
      hasTreatment: tieneTratamiento,
      progressPct: tieneTratamiento ? Number((40 + (seed % 60)).toFixed(1)) : null,
      improvementPct: tieneTratamiento ? Number((-5 + (seed % 30)).toFixed(1)) : null,
      periodLabel: tieneTratamiento ? 'Ultimas 4 semanas' : 'Sin datos',
    };
  });
}

/**
 * Devuelve la serie temporal de una parte del cuerpo concreta.
 * Mock: 12 puntos semanales con tendencia ligeramente positiva (deterministica por parte+dni).
 *
 * @param {string} dniPac
 * @param {string} bodyPartId
 * @param {string|null} _javaToken
 * @returns {Promise<Array>} BodyPartMetric[]
 */
async function obtenerMetricasPorParte(dniPac, bodyPartId, _javaToken) {
  // Hash determinista para que la grafica sea estable entre llamadas (no cambia en cada refresh).
  let seed = 0;
  for (const c of `${dniPac}-${bodyPartId}`) seed = (seed * 31 + c.charCodeAt(0)) >>> 0;

  const hoy = new Date();
  const puntos = [];
  for (let i = 11; i >= 0; i -= 1) {
    const fecha = new Date(hoy.getTime() - i * 7 * 24 * 60 * 60 * 1000);
    const isoDate = fecha.toISOString().slice(0, 10);
    // Score crece con el tiempo (i menor = mas reciente = mayor score), con ruido.
    const base = 50 + (11 - i) * 3;
    const ruido = ((seed >> i) & 0x07) - 3;
    puntos.push({
      date: isoDate,
      score: Number((base + ruido).toFixed(1)),
      metricType: 'angulo_flexion',
    });
  }
  return puntos;
}

module.exports = { obtenerProgresoPorParte, obtenerMetricasPorParte };
