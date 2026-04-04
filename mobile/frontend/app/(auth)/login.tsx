import { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  Pressable,
  Image,
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
} from 'react-native';
import { useAuthStore } from '../../src/store/authStore';
import { useUserStore } from '../../src/store/userStore';
import { useErrorStore } from '../../src/store/errorStore';
import type { LoginCredentials } from '../../src/types/auth';

// Pantalla de inicio de sesion
export default function LoginScreen() {
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');

  const login = useAuthStore(function (s) { return s.login; });
  const isLoading = useAuthStore(function (s) { return s.isLoading; });
  const fetchProfile = useUserStore(function (s) { return s.fetchProfile; });
  const showError = useErrorStore(function (s) { return s.showError; });

  async function handleLogin() {
    if (!identifier.trim() || !password.trim()) return;

    const credentials: LoginCredentials = {
      identifier: identifier.trim(),
      password: password,
    };

    try {
      await login(credentials);
      await fetchProfile();
      // La navegacion se gestiona automaticamente por el AuthGuard en el root layout
    } catch (err: any) {
      showError(err);
    }
  }

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      className="flex-1 bg-background"
    >
      <ScrollView
        contentContainerStyle={{ flexGrow: 1 }}
        keyboardShouldPersistTaps="handled"
      >
        <View className="flex-1 justify-center items-center px-8">
          {/* Logotipo de la aplicacion */}
          <Image
            source={require('../../assets/rehabiapp-logo.png')}
            className="w-48 h-48 mb-10"
            resizeMode="contain"
          />

          {/* Campo: DNI o correo electronico */}
          <View className="w-full mb-4">
            <Text className="text-text-secondary text-sm mb-1 ml-1">
              DNI / Gmail
            </Text>
            <TextInput
              value={identifier}
              onChangeText={setIdentifier}
              placeholder="12345678A o correo@gmail.com"
              autoCapitalize="none"
              keyboardType="email-address"
              className="w-full min-h-12 bg-surface border border-primary-200 rounded-xl px-4 py-3 text-base text-text-primary"
            />
          </View>

          {/* Campo: Contrasena */}
          <View className="w-full mb-8">
            <Text className="text-text-secondary text-sm mb-1 ml-1">
              Contrasena
            </Text>
            <TextInput
              value={password}
              onChangeText={setPassword}
              placeholder="Introduce tu contrasena"
              secureTextEntry={true}
              className="w-full min-h-12 bg-surface border border-primary-200 rounded-xl px-4 py-3 text-base text-text-primary"
            />
          </View>

          {/* Boton de inicio de sesion */}
          <Pressable
            onPress={handleLogin}
            disabled={isLoading}
            className="w-full min-h-12 bg-primary-600 rounded-xl py-4 items-center justify-center"
          >
            {isLoading ? (
              <ActivityIndicator color="#FFFFFF" />
            ) : (
              <Text className="text-white text-base font-semibold">
                Iniciar Sesion
              </Text>
            )}
          </Pressable>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}
