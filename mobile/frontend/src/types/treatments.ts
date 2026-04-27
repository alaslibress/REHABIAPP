type TreatmentType = 'MEDICATION' | 'EXERCISE' | 'GAMIFICATION' | 'TEXT_INSTRUCTION';

export type Treatment = {
  id: string;
  codTrat: string;
  name: string;
  description: string | null;
  type: TreatmentType;
  visible: boolean;
  progressionLevel: number;
  disabilityCode: string;
  summary: string | null;
  materials: string[];
  medication: string[];
  documentUrl: string | null;
  hasDocument: boolean;
};

export type Disability = {
  id: string;
  codDis: string;   // same value as id — BFF maps id = codDis
  name: string;
  description: string | null;
  currentLevel: number;
};

export type TreatmentDocument = {
  fileName: string;
  mimeType: string;
  base64: string | null;
  url: string | null;
};

export type { TreatmentType };
