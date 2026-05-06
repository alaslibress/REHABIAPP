type Patient = {
  id: string;
  name: string;
  surname: string;
  email: string | null;
  dni: string;
  phone: string | null;
  birthDate: string | null;
  address: string | null;
  active: boolean;
  numSs: string | null;
  sexo: 'MASCULINO' | 'FEMENINO' | 'OTRO' | null;
  avatarDataUri: string | null;
};

type UserState = {
  patient: Patient | null;
  isLoading: boolean;
  fetchProfile: () => Promise<void>;
  clearProfile: () => void;
};

export type { Patient, UserState };
