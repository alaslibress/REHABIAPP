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
