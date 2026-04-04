type Patient = {
  id: string;
  name: string;
  surname: string;
  email: string;
  dni: string;
  phone: string | null;
  birthDate: string | null;
  address: string | null;
  active: boolean;
};

type UserState = {
  patient: Patient | null;
  isLoading: boolean;
  fetchProfile: () => Promise<void>;
  clearProfile: () => void;
};

export type { Patient, UserState };
