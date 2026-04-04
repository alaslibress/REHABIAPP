import { Link, Stack } from 'expo-router';
import { View, Text } from 'react-native';

// Pantalla de ruta no encontrada
export default function NotFoundScreen() {
  return (
    <>
      <Stack.Screen options={{ title: 'Pagina no encontrada' }} />
      <View className="flex-1 bg-background justify-center items-center px-6">
        <Text className="text-2xl font-bold text-text-primary mb-2">
          Pagina no encontrada
        </Text>
        <Text className="text-base text-text-secondary mb-8 text-center">
          La ruta que buscas no existe.
        </Text>
        <Link href="/" className="text-primary-600 text-base font-medium">
          Volver al inicio
        </Link>
      </View>
    </>
  );
}
