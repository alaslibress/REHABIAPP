// Tipos compartidos: enums globales del esquema GraphQL
'use strict';

const { gql } = require('graphql-tag');

const commonTypeDefs = gql`
  # Tipos raiz — deben definirse antes de cualquier extension en los demas archivos
  type Query
  type Mutation

  # Codigos de error estandarizados — coinciden con ErrorCode del frontend
  enum ErrorCode {
    INVALID_CREDENTIALS
    ACCOUNT_DEACTIVATED
    TOKEN_EXPIRED
    TOKEN_INVALID
    NETWORK_ERROR
    PATIENT_NOT_FOUND
    APPOINTMENT_CONFLICT
    APPOINTMENT_NOT_FOUND
    VALIDATION_ERROR
    INTERNAL_ERROR
  }

  # Estado de una cita medica
  enum AppointmentStatus {
    SCHEDULED
    COMPLETED
    CANCELLED
  }
`;

module.exports = commonTypeDefs;
