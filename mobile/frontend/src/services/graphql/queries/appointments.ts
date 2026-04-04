import { gql } from '@apollo/client';

export const GET_MY_APPOINTMENTS = gql`
  query GetMyAppointments($status: AppointmentStatus, $upcoming: Boolean) {
    myAppointments(status: $status, upcoming: $upcoming) {
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
