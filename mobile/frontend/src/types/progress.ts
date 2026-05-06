export type BodyPartId =
  | 'HEAD' | 'NECK' | 'TORSO'
  | 'LEFT_SHOULDER' | 'RIGHT_SHOULDER'
  | 'LEFT_ARM' | 'RIGHT_ARM'
  | 'LEFT_HAND' | 'RIGHT_HAND'
  | 'LEFT_HIP' | 'RIGHT_HIP'
  | 'LEFT_LEG' | 'RIGHT_LEG'
  | 'LEFT_FOOT' | 'RIGHT_FOOT';

export type BodyPartProgress = {
  id: BodyPartId;
  name: string;
  hasTreatment: boolean;
  progressPct: number;
  improvementPct: number;
  periodLabel: string;
};

export type BodyPartMetric = {
  date: string;
  score: number;
  metricType: string;
};
