import { Tabs } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { useTheme } from '../../src/utils/theme';
import { useFontScale } from '../../src/utils/fontScale';

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
  { name: 'appointments', title: 'Citas', icon: 'calendar-outline', iconFocused: 'calendar' },
  { name: 'games', title: 'Juegos', icon: 'game-controller-outline', iconFocused: 'game-controller' },
  { name: 'treatments', title: 'Cura', icon: 'medkit-outline', iconFocused: 'medkit' },
  { name: 'progress', title: 'Progreso', icon: 'bar-chart-outline', iconFocused: 'bar-chart' },
  { name: 'profile', title: 'Perfil', icon: 'person-outline', iconFocused: 'person' },
  { name: 'settings', title: 'Ajustes', icon: 'settings-outline', iconFocused: 'settings' },
];

export default function TabsLayout() {
  const { scheme } = useTheme();
  const scale = useFontScale();

  const isDark = scheme === 'dark';

  const headerBg = isDark ? '#111827' : '#FFFFFF';
  const tabBarBg = isDark ? '#111827' : '#FFFFFF';
  const tabBarBorder = isDark ? '#1F2937' : '#E2E8F0';
  const headerTextColor = isDark ? '#F1F5F9' : '#1E293B';
  const inactiveTint = isDark ? '#94A3B8' : '#64748B';

  return (
    <Tabs
      screenOptions={{
        tabBarActiveTintColor: '#2563EB',
        tabBarInactiveTintColor: inactiveTint,
        tabBarStyle: {
          backgroundColor: tabBarBg,
          borderTopWidth: 1,
          borderTopColor: tabBarBorder,
          height: 60,
          paddingBottom: 8,
          paddingTop: 4,
        },
        tabBarLabelStyle: {
          fontSize: 11 * scale,
          fontFamily: 'Inter_500Medium',
        },
        headerShown: true,
        headerStyle: {
          backgroundColor: headerBg,
          elevation: 2,
          shadowColor: '#000',
          shadowOffset: { width: 0, height: 1 },
          shadowOpacity: isDark ? 0.3 : 0.1,
          shadowRadius: 2,
        },
        headerTintColor: headerTextColor,
        headerTitleStyle: {
          fontFamily: 'Inter_600SemiBold',
          fontSize: 18 * scale,
          color: headerTextColor,
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
