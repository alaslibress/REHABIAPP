import { Tabs } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';

// Tipo para los nombres de iconos de Ionicons
type TabIconName = keyof typeof Ionicons.glyphMap;

// Configuracion de cada pestana de navegacion
type TabConfig = {
  name: string;
  title: string;
  icon: TabIconName;
  iconFocused: TabIconName;
};

const TAB_CONFIG: TabConfig[] = [
  { name: 'index', title: 'Inicio', icon: 'home-outline', iconFocused: 'home' },
  { name: 'profile', title: 'Perfil', icon: 'person-outline', iconFocused: 'person' },
  { name: 'settings', title: 'Ajustes', icon: 'settings-outline', iconFocused: 'settings' },
  { name: 'appointments', title: 'Citas', icon: 'calendar-outline', iconFocused: 'calendar' },
  { name: 'progress', title: 'Progreso', icon: 'bar-chart-outline', iconFocused: 'bar-chart' },
  { name: 'games', title: 'Juegos', icon: 'game-controller-outline', iconFocused: 'game-controller' },
  { name: 'treatments', title: 'Tratamientos', icon: 'medkit-outline', iconFocused: 'medkit' },
];

export default function TabsLayout() {
  return (
    <Tabs
      screenOptions={{
        tabBarActiveTintColor: '#2563EB',
        tabBarInactiveTintColor: '#64748B',
        tabBarStyle: {
          backgroundColor: '#FFFFFF',
          borderTopWidth: 1,
          borderTopColor: '#E2E8F0',
          height: 60,
          paddingBottom: 8,
          paddingTop: 4,
        },
        tabBarLabelStyle: {
          fontSize: 11,
          fontWeight: '500',
        },
        headerShown: true,
        headerStyle: {
          backgroundColor: '#FFFFFF',
          elevation: 2,
          shadowColor: '#000',
          shadowOffset: { width: 0, height: 1 },
          shadowOpacity: 0.1,
          shadowRadius: 2,
        },
        headerTintColor: '#1E293B',
        headerTitleStyle: {
          fontWeight: '600',
          fontSize: 18,
        },
      }}
    >
      {TAB_CONFIG.map(function (tab) {
        return (
          <Tabs.Screen
            key={tab.name}
            name={tab.name}
            options={{
              title: tab.title,
              tabBarIcon: function ({ focused, color, size }) {
                const iconName = focused ? tab.iconFocused : tab.icon;
                return <Ionicons name={iconName} size={size} color={color} />;
              },
            }}
          />
        );
      })}
    </Tabs>
  );
}
