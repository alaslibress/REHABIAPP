import { Switch } from 'react-native';
import { SettingsRow } from './SettingsRow';

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

  return (
    <SettingsRow
      label={label}
      description={description}
      right={
        <Switch
          value={value}
          onValueChange={onChange}
          disabled={disabled}
          trackColor={{ false: '#E2E8F0', true: '#93C5FD' }}
          thumbColor={value ? '#2563EB' : '#94A3B8'}
        />
      }
    />
  );
}
