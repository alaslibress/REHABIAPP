import { Text } from 'react-native';
import type { TextProps } from 'react-native';
import { useFontScale } from '../utils/fontScale';

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

// Componente de texto que aplica escala de fuente global y tipografia Inter
export function AppText(props: AppTextProps) {
  const { variant = 'body', weight = 'regular', style, children, ...rest } = props;
  const scale = useFontScale();

  const baseFontSize = VARIANT_SIZES[variant];
  const fontFamily = WEIGHT_FAMILIES[weight];

  return (
    <Text
      style={[
        { fontFamily, fontSize: baseFontSize * scale },
        style,
      ]}
      {...rest}
    >
      {children}
    </Text>
  );
}
