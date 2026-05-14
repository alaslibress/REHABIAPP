import { Text } from 'react-native';
import type { TextProps } from 'react-native';
import { useFontScale } from '../utils/fontScale';
import { useTheme } from '../utils/theme';

// Tamaños base por variante (en dp)
const VARIANT_SIZES = {
  body: 16,
  title: 24,
  subtitle: 18,
  label: 14,
  caption: 12,
};

// Familias tipograficas por peso (deben coincidir con los nombres cargados por useFonts)
const WEIGHT_FAMILIES = {
  regular: 'Inter_400Regular',
  medium: 'Inter_500Medium',
  semibold: 'Inter_600SemiBold',
  bold: 'Inter_700Bold',
};

type AppTextProps = TextProps & {
  variant?: keyof typeof VARIANT_SIZES;
  weight?: keyof typeof WEIGHT_FAMILIES;
  className?: string;
  children: React.ReactNode;
};

// Texto de la app con escala global, fuente Inter y color por defecto segun tema.
// Si el caller pasa una className con text-* o un style.color, esos valores ganan
// sobre el color por defecto gracias al orden de merging de StyleSheet.
export function AppText(props: AppTextProps) {
  const { variant = 'body', weight = 'regular', style, children, ...rest } = props;
  const scale = useFontScale();
  const { scheme } = useTheme();

  const baseFontSize = VARIANT_SIZES[variant];
  const fontFamily = WEIGHT_FAMILIES[weight];

  // Color por defecto: blanco roto en oscuro (#F1F5F9 = text-primary-dark),
  // gris pizarra en claro (#1E293B = text-primary).
  const defaultColor = scheme === 'dark' ? '#F1F5F9' : '#1E293B';

  return (
    <Text
      style={[
        { fontFamily, fontSize: baseFontSize * scale, color: defaultColor },
        style,
      ]}
      {...rest}
    >
      {children}
    </Text>
  );
}
