type TreatmentType = 'MEDICATION' | 'EXERCISE' | 'GAMIFICATION' | 'TEXT_INSTRUCTION';

type Treatment = {
  id: string;
  name: string;
  description: string | null;
  type: TreatmentType;
  visible: boolean;
  progressionLevel: number;
};

type Disability = {
  id: string;
  name: string;
  description: string | null;
  currentLevel: number;
};

export type { Treatment, TreatmentType, Disability };
