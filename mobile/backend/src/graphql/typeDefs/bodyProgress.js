// TypeDefs del progreso por parte del cuerpo
// Alimenta el componente BodyDiagram + ProgressChartModal del frontend
'use strict';

const { gql } = require('graphql-tag');

const bodyProgressTypeDefs = gql`
  # Identificadores de partes del cuerpo. Coincide 1:1 con frontend types/progress.ts BodyPartId.
  enum BodyPartId {
    HEAD
    NECK
    TORSO
    LEFT_SHOULDER
    RIGHT_SHOULDER
    LEFT_ARM
    RIGHT_ARM
    LEFT_HAND
    RIGHT_HAND
    LEFT_HIP
    RIGHT_HIP
    LEFT_LEG
    RIGHT_LEG
    LEFT_FOOT
    RIGHT_FOOT
  }

  # Resumen de progreso por parte del cuerpo (para el BodyDiagram clickable)
  type BodyPartProgress {
    id: BodyPartId!
    # Etiqueta legible en castellano (p.ej. "Cadera derecha")
    name: String!
    # True si el paciente tiene tratamiento activo afectando esta parte
    hasTreatment: Boolean!
    # Progreso acumulado [0..100]. Null si no hay datos suficientes.
    progressPct: Float
    # Mejora porcentual contra baseline [-100..+100]. Null si no hay baseline.
    improvementPct: Float
    # Etiqueta del periodo agregado (p.ej. "Ultimas 4 semanas").
    periodLabel: String!
  }

  # Punto de la serie temporal para una parte del cuerpo
  type BodyPartMetric {
    # Fecha en ISO8601 'YYYY-MM-DD'.
    date: String!
    # Valor numerico (escala depende de metricType — el frontend lo muestra tal cual).
    score: Float!
    # Identificador de la metrica (p.ej. 'angulo_flexion', 'fuerza_isometrica', 'dolor_evaluado').
    metricType: String!
  }

  extend type Query {
    # Resumen de progreso por parte del cuerpo del paciente autenticado.
    # Util para pintar el BodyDiagram con colores segun progressPct.
    myBodyPartProgress: [BodyPartProgress!]!

    # Serie temporal de una parte del cuerpo concreta. Usado por ProgressChartModal.
    bodyPartMetrics(bodyPartId: BodyPartId!): [BodyPartMetric!]!
  }
`;

module.exports = bodyProgressTypeDefs;
