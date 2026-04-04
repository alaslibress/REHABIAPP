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

export type { Appointment, AppointmentStatus };
