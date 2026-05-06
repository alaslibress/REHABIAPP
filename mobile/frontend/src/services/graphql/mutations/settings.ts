import { gql } from '@apollo/client';

// Registra el token de notificaciones push del dispositivo en el BFF
export const REGISTER_DEVICE_TOKEN = gql`
  mutation RegisterDeviceToken($token: String!, $platform: String!) {
    registerDeviceToken(token: $token, platform: $platform)
  }
`;

// Elimina el token de notificaciones push del dispositivo
export const UNREGISTER_DEVICE_TOKEN = gql`
  mutation UnregisterDeviceToken($token: String!) {
    unregisterDeviceToken(token: $token)
  }
`;
