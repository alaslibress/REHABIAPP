import { Switch } from 'react-native';
import { SettingsRow } from './SettingsRow';
import { useTheme } from '../utils/theme';

type ToggleRowProps = {
  label: string;
  description?: string;
  value: boolean;
  onChange: (v: boolean) => void;
  disabled?: boolean;
};

// Fila de ajuste con interruptor (Switch) a la derecha
export function ToggleRow(props: ToggleRowProps) {
  const { label, description, value, onChange, disabled } = props;
  const { theme } = useTheme();

  return (
    <SettingsRow
      label={label}
      description={description}
      right={
        <Switch
          value={value}
          onValueChange={onChange}
          disabled={disabled}
          trackColor={{ false: theme.border2, true: theme.accent2 }}
          thumbColor="#FFFFFF"
        />
      }
    />
  );
}
