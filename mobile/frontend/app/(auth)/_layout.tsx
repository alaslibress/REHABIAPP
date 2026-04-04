import { Stack } from 'expo-router';

// Layout simple para las pantallas de autenticacion
export default function AuthLayout() {
  return (
    <Stack screenOptions={{ headerShown: false }} />
  );
}
