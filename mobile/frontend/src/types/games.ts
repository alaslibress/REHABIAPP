type GameMetrics = {
  accuracy: number | null;
  reactionTime: number | null;
  completionRate: number | null;
};

type GameSession = {
  id: string;
  gameName: string;
  playedAt: string;
  score: number | null;
  duration: number | null;
  metrics: GameMetrics | null;
};

type ProgressSummary = {
  totalSessions: number;
  averageScore: number | null;
  improvementRate: number | null;
  lastSessionDate: string | null;
};

export type { GameMetrics, GameSession, ProgressSummary };
