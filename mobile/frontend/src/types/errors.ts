type AppError = {
  title: string;       // Siempre "Error"
  subtitle: string;    // Nombre especifico del problema
  message: string;     // Descripcion detallada
  code: ErrorCode;
};

type ErrorCode =
  | 'INVALID_CREDENTIALS'
  | 'INVALID_IDENTIFIER'
  | 'WRONG_PASSWORD'
  | 'ACCOUNT_DEACTIVATED'
  | 'TOKEN_EXPIRED'
  | 'TOKEN_INVALID'
  | 'NETWORK_ERROR'
  | 'PATIENT_NOT_FOUND'
  | 'APPOINTMENT_CONFLICT'
  | 'APPOINTMENT_NOT_FOUND'
  | 'VALIDATION_ERROR'
  | 'INTERNAL_ERROR'
  | 'NO_GAMES_ASSIGNED'
  | 'NO_TREATMENTS_ASSIGNED'
  | 'DOCUMENT_DOWNLOAD_FAILED'
  | 'APPOINTMENT_REQUEST_INVALID_CONTACT'
  | 'NOTIFICATION_PERMISSION_DENIED'
  | 'BODY_PART_NO_DATA'
  | 'APPOINTMENT_REQUEST_CONFLICT';

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
