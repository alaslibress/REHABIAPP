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
  // Rehabilitacion movilidad fina manos (juego PIANO) — paciente Juan tiene
  // lesion solo en la mano derecha.
  'M-PIANO': ['RIGHT_HAND'],
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

  // Pedimos el progreso real al API (agregado desde MongoDB via /data).
  // Si falla, seguimos sin metricas reales — el muneco se pinta igualmente.
  let progresoReal = [];
  try {
    const resp = await apiClient.get(`/api/pacientes/${dniPac}/progreso`, javaToken);
    if (Array.isArray(resp)) progresoReal = resp;
  } catch {
    // El upstream puede estar caido o no haber sesiones — caso normal.
  }

  // Agrupamos los datos reales por parte del cuerpo (case-insensitive).
  // Para cada parte calculamos progressPct y improvementPct a partir de las
  // entradas reales: progressPct = score mas reciente (clamp 0-100),
  // improvementPct = ((ultimo - primero) / primero) * 100.
  const datosPorParte = {};
  for (const trat of progresoReal) {
    if (!trat.parteCuerpo) continue;
    const etiquetaTrat = trat.parteCuerpo.toLowerCase();
    const entradas = Array.isArray(trat.entradas) ? trat.entradas : [];
    if (entradas.length === 0) continue;
    if (!datosPorParte[etiquetaTrat]) datosPorParte[etiquetaTrat] = [];
    for (const e of entradas) {
      if (e?.valor != null && e?.fecha) {
        datosPorParte[etiquetaTrat].push({ fecha: e.fecha, valor: Number(e.valor) });
      }
    }
  }
  for (const k of Object.keys(datosPorParte)) {
    datosPorParte[k].sort(function (a, b) { return String(a.fecha).localeCompare(String(b.fecha)); });
  }

  return TODAS_LAS_PARTES.map((parte) => {
    const tieneTratamiento = partesAfectadas.has(parte);
    const etiqueta = (ETIQUETAS_PARTE[parte] || '').toLowerCase();
    const datos = datosPorParte[etiqueta] || [];

    let progressPct = null;
    let improvementPct = null;
    let periodLabel = 'Sin datos';

    if (tieneTratamiento) {
      if (datos.length > 0) {
        // progressPct simboliza adherencia al plan de rehabilitacion:
        // 10 sesiones completadas equivalen al 100% (objetivo clinico tipico).
        // Asi 1 sesion sale 10%, 5 sesiones 50%, etc. — visualmente honesto.
        const objetivoSesiones = 10;
        progressPct = Number(Math.min(100, (datos.length / objetivoSesiones) * 100).toFixed(1));

        // improvementPct: variacion porcentual del score entre primera y ultima
        // entrada. Con una sola sesion no hay base de comparacion -> 0.
        if (datos.length >= 2) {
          const primero = datos[0].valor;
          const ultimo = datos[datos.length - 1].valor;
          if (primero !== 0) {
            improvementPct = Number((((ultimo - primero) / primero) * 100).toFixed(1));
          } else {
            improvementPct = 0;
          }
        } else {
          improvementPct = 0;
        }
        const sufijo = datos.length === 1 ? 'sesion registrada' : 'sesiones registradas';
        periodLabel = `${datos.length} ${sufijo}`;
      } else {
        // Tratamiento asignado pero sin sesiones aun.
        progressPct = 0;
        improvementPct = 0;
        periodLabel = 'Aun sin sesiones';
      }
    }

    return {
      id: parte,
      name: ETIQUETAS_PARTE[parte],
      hasTreatment: tieneTratamiento,
      progressPct,
      improvementPct,
      periodLabel,
    };
  });
}

/**
 * Devuelve la serie temporal real de una parte del cuerpo concreta.
 *
 * Estrategia: consume `GET /api/pacientes/{dni}/progreso` (que a su vez
 * agrega desde MongoDB via /data) y filtra los tratamientos cuya
 * `parteCuerpo` coincide con el bodyPartId solicitado.
 *
 * Si el API no responde o no hay entradas para esa parte, devuelve [] —
 * la pantalla muestra un grafico vacio en lugar de datos inventados.
 *
 * @param {string} dniPac
 * @param {string} bodyPartId  Id frontend (ej "RIGHT_HAND")
 * @param {string|null} javaToken
 * @returns {Promise<Array>} BodyPartMetric[]
 */
async function obtenerMetricasPorParte(dniPac, bodyPartId, javaToken) {
  // Mapeo bodyPartId -> etiqueta legible que usa el pipeline (case-insensitive).
  const etiquetaEsperada = (ETIQUETAS_PARTE[bodyPartId] || '').toLowerCase();
  if (!etiquetaEsperada) return [];

  let tratamientos;
  try {
    tratamientos = await apiClient.get(`/api/pacientes/${dniPac}/progreso`, javaToken);
  } catch {
    return [];
  }
  if (!Array.isArray(tratamientos)) return [];

  // Filtramos tratamientos cuya parteCuerpo coincide con la pedida.
  // Concatenamos las entradas de todos los tratamientos que machean para que
  // el grafico muestre la evolucion completa de esa zona corporal.
  const puntos = [];
  for (const trat of tratamientos) {
    const parte = (trat.parteCuerpo || '').toLowerCase();
    if (parte !== etiquetaEsperada) continue;
    const entradas = Array.isArray(trat.entradas) ? trat.entradas : [];
    for (const e of entradas) {
      if (e?.valor == null || !e?.fecha) continue;
      puntos.push({
        date: String(e.fecha).slice(0, 10),
        score: Number(Number(e.valor).toFixed(2)),
        metricType: trat.metricaNombre || 'score',
      });
    }
  }
  // Orden cronologico ascendente — el grafico espera fechas ordenadas.
  puntos.sort(function (a, b) { return a.date.localeCompare(b.date); });
  return puntos;
}

module.exports = { obtenerProgresoPorParte, obtenerMetricasPorParte };
