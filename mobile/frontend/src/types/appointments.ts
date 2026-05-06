type AppointmentStatus = 'SCHEDULED' | 'COMPLETED' | 'CANCELLED';

type Appointment = {
  id: string;
  date: string;
  time: string;
  practitionerName: string;
  practitionerSpecialty: string | null;
  status: AppointmentStatus;
  notes: string | null;
};

type RequestAppointmentInput = {
  fechaPreferida: string;  // YYYY-MM-DD
  horaPreferida: string;   // HH:mm
  motivo: string;
  telefono?: string | null;
  email?: string | null;
};

type AppointmentRequest = {
  id: string;
  fechaPreferida: string;
  horaPreferida: string;
  motivo: string;
  estado: 'PENDING' | 'CONFIRMED' | 'REJECTED';
  createdAt: string;
};

export type { Appointment, AppointmentStatus, RequestAppointmentInput, AppointmentRequest };
