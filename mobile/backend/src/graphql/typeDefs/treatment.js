// TypeDefs de tratamientos del paciente
'use strict';

const { gql } = require('graphql-tag');

const treatmentTypeDefs = gql`
  # Tratamiento terapeutico asignado al paciente
  type Treatment {
    id: ID!
    # Codigo del tratamiento (igual a id en el modelo actual; se mantienen ambos
    # para compatibilidad con el frontend, que los usa indistintamente).
    codTrat: String!
    name: String!
    description: String
    type: String!
    visible: Boolean!
    progressionLevel: Int!
    # Codigo de la discapacidad asociada al tratamiento (FK a Disability.id).
    # Null si el tratamiento no esta vinculado a una discapacidad concreta.
    disabilityCode: String
    # Resumen breve para mostrar en la card de la lista de tratamientos.
    summary: String
    # Materiales necesarios para realizar el tratamiento. Lista vacia si no aplica.
    materials: [String!]!
    # Medicacion asociada al tratamiento (nombre + posologia). Lista vacia si no aplica.
    medication: [String!]!
    # URL absoluta al PDF del protocolo. Null si no hay PDF subido.
    documentUrl: String
    # True si el tratamiento tiene PDF descargable via la query treatmentDocument.
    hasDocument: Boolean!
  }

  # Documento descargable asociado a un tratamiento (PDF de protocolo, video, etc.)
  type TreatmentDocument {
    # Nombre sugerido al guardar el archivo en el dispositivo.
    fileName: String!
    # MIME type. Para PDFs siempre 'application/pdf'.
    mimeType: String!
    # Contenido en base64 (incluye el PDF entero). Null si el frontend debe usar la url.
    base64: String
    # URL absoluta al recurso. Null si el contenido va en base64.
    url: String
  }

  # PDF del protocolo de un tratamiento serializado en base64.
  # El frontend lo decodifica y lo escribe a expo-file-system para abrir/compartir.
  # Limite duro: 10 MB. Si se excede, el resolver devuelve un error VALIDATION_ERROR.
  type TreatmentPdfPayload {
    codTrat: String!
    filename: String!
    sizeBytes: Int!
    base64Content: String!
  }

  extend type Query {
    # Tratamientos del paciente, filtrable por discapacidad y nivel de progresion
    myTreatments(disabilityId: ID, level: Int): [Treatment!]!

    # Descarga el PDF del protocolo del tratamiento.
    # El BFF llama a GET /api/tratamientos/{cod}/pdf y devuelve los bytes en base64.
    treatmentPdf(codTrat: String!): TreatmentPdfPayload

    # Descarga el documento del tratamiento. Para PDFs de hasta 10MB se devuelve base64;
    # para tamanos mayores se devuelve url (no implementado aun en mock).
    treatmentDocument(codTrat: ID!): TreatmentDocument
  }
`;

module.exports = treatmentTypeDefs;
