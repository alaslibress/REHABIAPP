import { gql } from '@apollo/client';

export const BOOK_APPOINTMENT = gql`
  mutation BookAppointment($date: String!, $time: String!, $practitionerId: ID!) {
    bookAppointment(date: $date, time: $time, practitionerId: $practitionerId) {
      id
      date
      time
      practitionerName
      practitionerSpecialty
      status
      notes
    }
  }
`;

export const CANCEL_APPOINTMENT = gql`
  mutation CancelAppointment($appointmentId: ID!) {
    cancelAppointment(appointmentId: $appointmentId) {
      id
      status
    }
  }
`;

export const REQUEST_APPOINTMENT = gql`
  mutation RequestAppointment(
    $fechaPreferida: String!
    $horaPreferida: String!
    $motivo: String!
    $telefono: String
    $email: String
  ) {
    requestAppointment(
      fechaPreferida: $fechaPreferida
      horaPreferida: $horaPreferida
      motivo: $motivo
      telefono: $telefono
      email: $email
    ) {
      id
      fechaPreferida
      horaPreferida
      motivo
      estado
      createdAt
    }
  }
`;
