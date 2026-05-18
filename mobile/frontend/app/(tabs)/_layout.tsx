import { Tabs } from 'expo-router';
import {
  House,
  CalendarBlank,
  GameController,
  FirstAidKit,
  ChartBar,
  UserCircle,
  GearSix,
  type IconProps,
} from 'phosphor-react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useTheme } from '../../src/utils/theme';
import { useFontScale } from '../../src/utils/fontScale';

// Configuracion de cada pestana de navegacion (rebrand Phosphor)
type TabConfig = {
  name: string;
  title: string;
  Icon: React.ComponentType<IconProps>;
};

const TAB_CONFIG: TabConfig[] = [
  { name: 'index', title: 'Inicio', Icon: House },
  { name: 'appointments', title: 'Citas', Icon: CalendarBlank },
  { name: 'games', title: 'Juegos', Icon: GameController },
  { name: 'treatments', title: 'Cura', Icon: FirstAidKit },
  { name: 'progress', title: 'Progreso', Icon: ChartBar },
  { name: 'profile', title: 'Perfil', Icon: UserCircle },
  { name: 'settings', title: 'Ajustes', Icon: GearSix },
];

export default function TabsLayout() {
  const { theme } = useTheme();
  const scale = useFontScale();
  const insets = useSafeAreaInsets();

  // Safe area bottom: la tab-nav no debe quedar bajo la zona de gestos.
  return (
    <Tabs
      screenOptions={{
        tabBarActiveTintColor: theme.accent,
        tabBarInactiveTintColor: theme.text3,
        tabBarStyle: {
          backgroundColor: theme.elev1,
          borderTopWidth: 1,
          borderTopColor: theme.divider,
          height: 56 + insets.bottom,
          paddingBottom: insets.bottom + 6,
          paddingTop: 4,
          elevation: 8,
          shadowColor: theme.shadow.color,
          shadowOffset: { width: 0, height: -2 },
          shadowOpacity: theme.shadow.opacity,
          shadowRadius: 4,
        },
        tabBarLabelStyle: {
          fontSize: 10 * scale,
          fontFamily: 'Inter_600SemiBold',
        },
        headerShown: true,
        headerStyle: {
          backgroundColor: theme.bg,
          elevation: 2,
          shadowColor: theme.shadow.color,
          shadowOffset: { width: 0, height: 1 },
          shadowOpacity: theme.shadow.opacity,
          shadowRadius: 2,
        },
        headerTintColor: theme.text,
        headerTitleStyle: {
          fontFamily: 'Inter_700Bold',
          fontSize: 18 * scale,
          color: theme.text,
        },
      }}
    >
      {TAB_CONFIG.map(function (tab) {
        const Icon = tab.Icon;
        return (
          <Tabs.Screen
            key={tab.name}
            name={tab.name}
            options={{
              title: tab.title,
              tabBarIcon: function ({ focused, color, size }) {
                return (
                  <Icon size={size} color={color} weight={focused ? 'fill' : 'regular'} />
                );
              },
            }}
          />
        );
      })}
    </Tabs>
  );
}
