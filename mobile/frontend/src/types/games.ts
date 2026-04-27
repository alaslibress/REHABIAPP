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

export type GameDifficulty = 'EASY' | 'MEDIUM' | 'HARD';

export type AssignedGame = {
  id: string;
  name: string;
  description: string;
  thumbnailUrl: string | null;
  webglUrl: string | null;
  difficulty: GameDifficulty;
  assignedAt: string;
};

export type { GameMetrics, GameSession, ProgressSummary };
