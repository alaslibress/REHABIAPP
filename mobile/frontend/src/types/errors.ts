type AppError = {
  title: string;       // Siempre "Error"
  subtitle: string;    // Nombre especifico del problema
  message: string;     // Descripcion detallada
  code: ErrorCode;
};

type ErrorCode =
  | 'INVALID_CREDENTIALS'
  | 'ACCOUNT_DEACTIVATED'
  | 'TOKEN_EXPIRED'
  | 'TOKEN_INVALID'
  | 'NETWORK_ERROR'
  | 'PATIENT_NOT_FOUND'
  | 'APPOINTMENT_CONFLICT'
  | 'APPOINTMENT_NOT_FOUND'
  | 'VALIDATION_ERROR'
  | 'INTERNAL_ERROR';

type ErrorPopupProps = {
  error: AppError;
  visible: boolean;
  onAccept: () => void;
  onCancel: () => void;
};

type ErrorState = {
  currentError: AppError | null;
  isVisible: boolean;
  showError: (error: AppError) => void;
  hideError: () => void;
};

export type { AppError, ErrorCode, ErrorPopupProps, ErrorState };
